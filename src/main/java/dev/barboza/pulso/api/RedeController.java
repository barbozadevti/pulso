package dev.barboza.pulso.api;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.barboza.pulso.config.QrCodeSvg;

/** Endereço do Pulso na rede local, com QR Code, para abrir no celular conectado ao mesmo Wi-Fi. */
@RestController
@RequestMapping("/api/rede")
public class RedeController {

    public record Endereco(String url, boolean local) {
    }

    private final int porta;

    public RedeController(@Value("${server.port}") int porta) {
        this.porta = porta;
    }

    @GetMapping
    Endereco endereco() {
        String ip = ipDaRede();
        return new Endereco(ip == null ? "http://localhost:" + porta : "http://" + ip + ":" + porta, ip == null);
    }

    @GetMapping(value = "/qr.svg", produces = "image/svg+xml")
    ResponseEntity<String> qr() {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/svg+xml"))
                .cacheControl(CacheControl.noCache()).body(QrCodeSvg.gerar(endereco().url()));
    }

    private static String ipDaRede() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                String nome = ni.getDisplayName().toLowerCase();
                if (nome.contains("virtual") || nome.contains("vethernet") || nome.contains("wsl")
                        || nome.contains("docker") || nome.contains("vmware")) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignorado) {
            // sem rede: cai no localhost
        }
        return null;
    }
}
