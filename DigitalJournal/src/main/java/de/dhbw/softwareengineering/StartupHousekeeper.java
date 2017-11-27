package de.dhbw.softwareengineering;

import de.dhbw.softwareengineering.utilities.FileConfiguration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
class StartupHousekeeper {

    @EventListener(ContextRefreshedEvent.class)
    void contextRefreshedEvent() {

        initEmailConfiguration();
        initMySQLConfiguration();
        initEmailTemplate();

    }

    private void initMySQLConfiguration(){

        File configurationDirectory = new File("." + File.separator + "conf");
        if(!configurationDirectory.exists()) {
            configurationDirectory.mkdir();
        }

        FileConfiguration mysqlConfiguration = new FileConfiguration(new File(configurationDirectory.getAbsolutePath() + File.separator + "mysql.conf"));

        mysqlConfiguration.setDefaultValue("user", "root");
        mysqlConfiguration.setDefaultValue("database", "digital_journal");
        mysqlConfiguration.setDefaultValue("password", "asdf");
        mysqlConfiguration.setDefaultValue("port", "3306");
        mysqlConfiguration.setDefaultValue("hostname", "localhost");

        try {
            mysqlConfiguration.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initEmailConfiguration(){
        FileConfiguration emailConfiguration = new FileConfiguration(new File("." + File.separator + "conf" + File.separator + "mysql.conf"));

        emailConfiguration.setDefaultValue("host", "localhost");
        emailConfiguration.setDefaultValue("port", "587");
        emailConfiguration.setDefaultValue("simpleName", "DigitalJournal");
        emailConfiguration.setDefaultValue("accountName", "digitaljournal");
        emailConfiguration.setDefaultValue("password", "password");

        try {
            emailConfiguration.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initEmailTemplate(){
        File file = new File("." + File.separator + "emailTemplate.html");

        if(!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file,false))){
                writer.write("Hello {$username},<br/>");
                writer.newLine();
                writer.write("to finish the registration process click on the following link to confirm your email: <br/><br/>");
                writer.newLine();
                writer.newLine();
                writer.write("{§link} <br/><br/>");
                writer.newLine();
                writer.newLine();
                writer.write("Kind regards, <br/>");
                writer.newLine();
                writer.write("Your DigitalJournal-Team");

                writer.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
