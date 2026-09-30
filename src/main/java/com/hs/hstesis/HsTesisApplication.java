package com.hs.hstesis;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {
        org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration.class
})
public class HsTesisApplication {

    public static void main(String[] args) {
        String profiles = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "local");
        if (!java.util.Arrays.asList(profiles.split(",")).contains("azure")) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(entry -> {
                if (System.getenv(entry.getKey()) == null && System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
        }
        SpringApplication.run(HsTesisApplication.class, args);
    }
}
