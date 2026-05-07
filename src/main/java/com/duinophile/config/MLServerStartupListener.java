package com.duinophile.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class MLServerStartupListener {

    @EventListener(ApplicationReadyEvent.class)
    public void startMLServers() {
        System.out.println("==========================================");
        System.out.println("🤖 Triggering ML Backend Auto-Setup...");
        System.out.println("==========================================");
        
        try {
            File workingDir = new File("chatbot-ml");
            if (!workingDir.exists()) {
                // Fallback for IDEs running from the outer directory
                workingDir = new File("duinophile/chatbot-ml");
            }
            if (!workingDir.exists()) {
                System.out.println("⚠️ Warning: 'chatbot-ml' directory not found. Skipping ML setup.");
                return;
            }

            ProcessBuilder pb;
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass", "-File", "run_ml_servers.ps1");
            } else {
                pb = new ProcessBuilder("bash", "run_ml_servers.sh");
            }

            pb.directory(workingDir);
            pb.inheritIO();
            
            // Start the process asynchronously (does not block Spring Boot startup)
            pb.start();
            
        } catch (IOException e) {
            System.err.println("❌ Failed to run ML server startup script: " + e.getMessage());
        }
    }
}
