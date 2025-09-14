
# MicroSpingBoot – Lab 05 AREP

Este proyecto consiste en la construcción de un **framework web mínimo**, desplegado en AWS utilizando **EC2** y **Docker**.  
El objetivo fue **mejorar el framework propio** para que soportara **concurrencia** y un **apagado elegante**, cumpliendo con los requisitos del enunciado.

---

## 🚀 Estructura de archivos
```bash
MicroSpingBoot/
├─ pom.xml
├─ Dockerfile
├─ README.md
├─ webroot/                      # Archivos estáticos servidos por HttpServer
│  └─ index.html
└─ src/
   └─ main/
      └─ java/
         └─ com/
            └─ mycompany/
               ├─ httpserver/
               │  ├─ HttpServer.java        
               │  ├─ HttpRequest.java        
               │  └─ HttpResponse.java       
               │
               ├─ microspingboot/
               │  └─ MicroSpingBoot.java    
               │
               ├─ microspingboot/anotations/
               │  ├─ RestController.java     
               │  ├─ GetMapping.java         
               │  └─ RequestParam.java       
               │
               └─ microspingboot/examples/
                  └─ GreetingController.java  # Ejemplo: /app/greeting?name=Juan -> "Hola Juan"


```
# Ejecución aplicación local

<img width="481" height="124" alt="image" src="https://github.com/user-attachments/assets/7f7f983f-f6b0-4635-8c93-cb146b7aec02" />


# ⚙️ Construcción de imagen
## Nuevo archivo dockerfile
```bash
FROM openjdk:17
 
WORKDIR /usrapp/bin
 
ENV PORT=6000
 
COPY /target/classes /usrapp/bin/classes
COPY /target/dependency /usrapp/bin/dependency
 
CMD ["java","-cp","./classes:./dependency/*","com.mycompany.microspingboot.MicroSpingBoot"]

## Crear contenerdor
docker build -t juanparroquiano/areplab05:latest .

## Ejecutar localmente
docker run -d -p 34000:6000 --name microspingboot microspingboot
## Navegar: http://localhost:35000/app/greeting?name=Juan

## Publicar en Docker Hub:
docker login
docker push juanparroquiano/areplab05:latest
```

# ⚙️ Generar automáticamente una configuración docker
```bash
Nuevo archivo "docker-compose.yml"
version: '2' 
 
services:
    web:
        build:
            context: .
            dockerfile: Dockerfile
        container_name: web
        ports:
        - "8087:6000"
    db:
        image: mongo:3.6.1
        container_name: db
        volumes:
          - mongodb:/data/db
          - mongodb_config:/data/configdb
        ports:
            - 27017:27017
        command: mongod
 
volumes:
  mongodb:
  mongodb_config:
```

## 🐳 Ejecute el docker compose:
docker-compose up -d

## Imagen subida a Docker Hub
<img width="1697" height="673" alt="image" src="https://github.com/user-attachments/assets/409ee8e5-f843-4070-a9b4-d9fc949e719f" />
https://hub.docker.com/repository/docker/juanparroquiano/areplab05/tags

# EC2 creada
<img width="1694" height="779" alt="image" src="https://github.com/user-attachments/assets/9ff948e4-3972-4bc8-94f3-577e7577eddc" />

## Subir imagen a EC2
docker run -d -p 42000:6000 --name firstdockerimageaws juanparroquiano/areplab05


