/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.microspingboot;

import com.mycompany.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collections;
import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * @author juan.parroquiano
 */
//@SpringBootApplication
public class MicroSpingBoot {

    /*public static void main(String[] args) throws IOException, URISyntaxException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        System.out.println("Starting MicroSpringBoot");
        
        HttpServer.staticfiles("/webroot");
        HttpServer.runServer(args);
    }*/
    
    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(MicroSpingBoot.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", getPort()));
        
        HttpServer.staticfiles("/webroot");
        HttpServer.runServer(args);
        
        //app.run(args);
    }

    private static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 35001;
    }

}
