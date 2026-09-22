package com.trimly.api.seeder;

import com.trimly.api.TrimlyBackendApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.TimeZone;

/**
 * Standalone Java Seeder Application Runner.
 * 
 * Runs the database seeder in headless non-web mode (WebApplicationType.NONE)
 * so it never conflicts with a running backend server on port 4000.
 */
public class DatabaseSeederApplication {

    static {
        System.setProperty("user.timezone", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        System.setProperty("user.timezone", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        ConfigurableApplicationContext context = new SpringApplicationBuilder(TrimlyBackendApplication.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .properties(
                        "spring.devtools.restart.enabled=false",
                        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect"
                )
                .run(args);

        DatabaseSeeder seeder = context.getBean(DatabaseSeeder.class);
        seeder.runSeeder();
        context.close();
        System.exit(0);
    }
}
