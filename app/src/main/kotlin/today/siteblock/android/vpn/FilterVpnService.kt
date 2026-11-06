package today.siteblock.android.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import today.siteblock.android.repository
import today.siteblock.android.system.Notifier
import today.siteblock.android.system.ServiceNames
import today.siteblock.core.DnsDecision
import today.siteblock.core.DnsFilter
import today.siteblock.core.DohProviders
import today.siteblock.core.DomainMatcher
import today.siteblock.core.Ipv4UdpPacket

/**
 * DNS-only local VPN. Only the fake DNS server and known DoH provider IPs are routed into the
 * tunnel, so ordinary traffic bypasses it entirely. Blocked names get NXDOMAIN; allowed queries
 * are relayed to the real network's resolver. Works for every app, including Telegram Custom Tabs.
 */
class FilterVpnService : VpnService() {

    private val scope = MainScope()
    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    private var collectJob: Job? = null
    private lateinit var resolver: UpstreamResolver

    @Volatile private var filter = DnsFilter(DomainMatcher(emptyList()))

    override fun onCreate() {
        super.onCreate()
        resolver = UpstreamResolver(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ServiceNames.ACTION_STOP) {
            shutdown()
            stopSelf()
            return START_NOT_STICKY
        }
        // ACTION_APPLY, the always-on/system start (action "android.net.VpnService"), or a restart (null intent).
        val enforcement = repository.enforcement.value
        if (!enforcement.filterActive) {
            shutdown()
            stopSelf()
            return START_NOT_STICKY
        }
        filter = DnsFilter(DomainMatcher(enforcement.filterDomains))
        if (tun == null && !establish()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (collectJob == null) {
            collectJob = scope.launch {
                repository.enforcement.collect { e ->
                    if (!e.filterActive) {
                        shutdown()
                        stopSelf()
                    } else {
                        filter = DnsFilter(DomainMatcher(e.filterDomains))
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        val wasWanted = repository.enforcement.value.filterActive
        shutdown()
        if (wasWanted) {
            val message = "Another VPN took over or the VPN was disconnected. Open SiteBlock to turn the web filter back on."
            repository.setVpnRunning(false, message)
            Notifier.showProblem(this, Notifier.ID_VPN, "Web filter stopped", message)
        }
        stopSelf()
    }

    override fun onDestroy() {
        shutdown()
        scope.cancel()
        super.onDestroy()
    }

    private fun establish(): Boolean {
        val builder = Builder()
            .setSession("SiteBlock web filter")
            .setMtu(MTU)
            .addAddress(TUN_ADDRESS, 32)
            .addDnsServer(FAKE_DNS)
            .addRoute(FAKE_DNS, 32)
            .setBlocking(true)
            .setMetered(false)
        DohProviders.ipv4Addresses.forEach { builder.addRoute(it, 32) }
        val pfd = try {
            builder.establish()
        } catch (e: Exception) {
            null
        }
        if (pfd == null) {
            repository.setVpnRunning(false, "VPN permission not granted. Open Setup to allow it.")
            return false
        }
        tun = pfd
        worker = Thread({ runLoop(pfd) }, "siteblock-dns").also { it.start() }
        repository.setVpnRunning(true, null)
        Notifier.clear(this, Notifier.ID_VPN)
        return true
    }

    /** Main-thread only. Idempotent. */
    private fun shutdown() {
        collectJob?.cancel()
        collectJob = null
        val wasRunning = tun != null
        try {
            tun?.close()
        } catch (e: IOException) {
            // already closed
        }
        tun = null
        worker?.interrupt()
        worker = null
        if (wasRunning) repository.setVpnRunning(false, null)
    }

    /** Runs on the worker thread. Never touches the repository. */
    private fun runLoop(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val pool: ExecutorService = Executors.newFixedThreadPool(FORWARD_THREADS)
        val buffer = ByteArray(MTU)
        try {
            while (!Thread.currentThread().isInterrupted) {
                val length = input.read(buffer)
                if (length < 0) break
                if (length == 0) continue
                when (val decision = filter.decide(buffer, length)) {
                    is DnsDecision.Respond -> synchronized(output) { output.write(decision.packet) }
                    is DnsDecision.Forward -> {
                        val request = decision.request
                        pool.execute { forward(request, output) }
                    }
                    DnsDecision.Drop -> Unit
                }
            }
        } catch (e: IOException) {
            // Tunnel closed by shutdown(); exit quietly.
        } finally {
            pool.shutdownNow()
        }
    }

    /** Relay one allowed query to a real resolver and write the answer back into the tunnel. */
    private fun forward(request: Ipv4UdpPacket, output: FileOutputStream) {
        val upstream = if (request.dstIp == FAKE_DNS) {
            resolver.upstream()
        } else {
            Upstream(InetAddress.getByAddress(request.dstAddr), null) // plain DNS to a routed DoH-provider IP
        }
        try {
            DatagramSocket().use { socket ->
                protect(socket)
                upstream.network?.bindSocket(socket)
                socket.soTimeout = UPSTREAM_TIMEOUT_MS
                socket.send(DatagramPacket(request.payload, request.payload.size, upstream.address, 53))
                val answer = ByteArray(MAX_DNS_RESPONSE)
                val packet = DatagramPacket(answer, answer.size)
                socket.receive(packet)
                val reply = request.reply(answer.copyOf(packet.length)).toBytes()
                synchronized(output) { output.write(reply) }
            }
        } catch (e: IOException) {
            // Timeout or network change. The app's resolver retries; refresh the upstream next time.
            resolver.invalidate()
        }
    }

    companion object {
        const val TUN_ADDRESS = "10.111.222.1"
        const val FAKE_DNS = "10.111.222.2"
        private const val MTU = 4500
        private const val MAX_DNS_RESPONSE = 4096
        private const val UPSTREAM_TIMEOUT_MS = 5_000
        private const val FORWARD_THREADS = 4
    }
}
