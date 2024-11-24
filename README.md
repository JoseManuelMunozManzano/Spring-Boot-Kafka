# INTRODUCTION TO KAFKA WITH SPRING BOOTS

Curso Udemy de Kafka con Spring Boot.

## Wiki & FAQ

`https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki`

## Instalar Kafka localmente y ejecutar el Broker

- Instalar Apache Kafka
  - En Mac
    - `https://kafka.apache.org/downloads`
    - Yo estoy utilizando la imagen Docker (para el server) para mi Raspberry Pi y he usado esta documentación:
      - De aquí cogí el `docker-compose.yml`: `https://medium.com/towards-data-engineering/unlock-the-power-of-apache-kafka-with-the-official-docker-image-5a65192e618b`
        - En `01-Docker-Compose` tengo también ese archivo y como ejecutar
      - De aquí cogí distintos mandatos: `https://hub.docker.com/r/apache/kafka`
      - Aquí indica como hacer que funcione en la LAN en distintos ordenadores y la WAN: `https://stackoverflow.com/questions/61101236/accessing-kafka-broker-from-outside-my-lan`
      - En la Raspberry Pi tuve que instalar Java
      - En la Raspberry Pi tuve también que instalar una versión de Kafka, para pruebas, siguiendo esta documentación:
        - Esta es la versión de Kafka (es más que posible que ya haya cambiado) que me descargué: `https://www.apache.org/dyn/closer.cgi?path=/kafka/3.9.0/kafka_2.13-3.9.0.tgz`
        - En la Raspberry Pi lo instalé en el directorio `~/tools/kafka` con el mandato: `tar -xzf kafka_2.13-3.9.0.tgz`
  - En Windows
    - No voy a hacerlo en Windows, pero dejo documentación
    - `https://github.com/lydtechconsulting/introduction-to-kafka-with-spring-boot/wiki/Installing-and-Running-Kafka-on-Windows`
    - `https://learn.microsoft.com/en-us/windows/wsl/install`

## Sending and Receiving

Vamos a enviar y recibir mensajes.

Para ello, utilizaremos un par de utilidades de línea de comandos suministradas por Kafka, una para publicar mensajes en un topic y otra para consumir mensajes de un topic.

Iniciamos tres terminales, una para el servidor Kafka, otra para el Consumer y otra para el Producer.

Esto lo hice en la Raspberry Pi, donde me dirigí a la carpeta `/home/pi/tools/kafka/kafka_2.13-3.9.0/bin` donde instalé la versión local de Kafka.

- Crear el topic en los contenedores Docker de Kafka. Estos contenedores de Kafka son el server. Esto solo hay que hacerlo una vez
  - `./kafka-topics.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --create --topic test-topic`
  - Para ver si se ha creado el topic, desde la versión local de Kafka ejecutar: `./kafka-topics.sh --list --bootstrap-server localhost:29092`
    - Tiene que salir el nombre `test-topic`
  - Para ver el mismo resultado desde Docker, ejecutar
    - Entrar al contenedor: `docker exec --workdir /opt/kafka/bin/ -it kafka-1 sh`
    - Ver el topic: `./kafka-topics.sh --list --bootstrap-server kafka-1:19092`
    - Tiene que salir el nombre `test-topic`
- Ejecutar la parte consumer en la terminal en el directorio donde tengo instalada la versión local de Kafka. Lo iniciamos para escuchar mensajes que pueden llegar sobre el topic
  - `./kafka-console-consumer.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --topic test-topic --from-beginning`
- Ejecuta la parte producer en otra terminal, en el directorio donde tengo instalada la versión local de Kafka. Importante indicar siempre el mismo nombre de topic
  - `./kafka-console-producer.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --topic test-topic`
  - Ahora, aquí en el producer, podemos escribir los mensajes que queramos, por ejemplo `my first message`
  - Esto debe aparecer en la terminal del consumer

Si ejecutamos en una nueva terminal de la Raspberry Pi el comando `docker container logs -f kafka-1` veremos los logs del contenedor que tiene el servidor Kafka. Ahí aparece cuando se crea un topic, cuando se modifica, cuando se cierra el consumer....

Para terminar, tanto en la terminal del consumer como en la terminal del producer como en la terminal con los logs del server, pulsar `Ctrl+C`.

IMPORTANTE: En la Raspberry Pi, en la carpeta `~./docker/kafka` tengo el fichero `docker-compose.yml` y el fichero `Ejecucion.txt` con más o menos la misma explicación que tengo aquí.

## CLI Tools

En la parte de envío y recibo de mensajes ya hemos visto varios usos de herramientas de CLI. Aquí se oficializa como se usa el CLI.

Las herramientas de CLI se encuentran en el directorio de instalación de Kafka, en el subdirectorio `bin`.

En mi Raspberry Pi, están en `/home/pi/tools/kafka/kafka_2.13-3.9.0/bin`.

Para ver cómo funcionan los distintos comandos de CLI y qué opciones tenemos, ejecutar desde ese directorio bin: `./<comando> --help`

### Arrancar y Parar el servidor Kafka

Como lo tengo en mi Raspberry Pi, en Docker, arrancando los contenedores automáticamente se arranca el server y parándolos se paran.

De todas formas, los comandos son, estando en la carpeta bin:

- Arrancar indicando la configuración
  - `./kafka-server-start.sh ../config/kraft/server.properties`
- Parar
  - `./kafka-server-stop.sh`

Estos comandos hay que ejecutarlos dentro del contenedor. Para entrar al contenedor kafka-1 ejecutar: `docker exec --workdir /opt/kafka/bin/ -it kafka-1 sh`.

### Topic Tool

El Topic tool puede usarse para listar todos los topics existentes, crear uno nuevo, modificar uno existente, eliminarlo y ver sus detalles.

- El comando para listar es, desde el directorio bin de mi instalación local de la Raspberry Pi:
  - `./kafka-topics.sh --bootstrap-server localhost:29092 --list`
  - Donde `29092` es el puerto del Kafka server que tengo corriendo en el contenedor Docker (uno de los tres que tengo) y `--list` es lo que quiero hacer con el topic, que es listarlo en este ejemplo.
- Para crear un nuevo topic, ejecutar:
  - `./kafka-topics.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --create --topic my.new.topic`
  - El nombre del topic se puede separar por puntos. Esto es muy común, pero indicar que Kafka trata el punto y el guión bajo como el mismo carácter
  - Los crea en mis contenedores Docker, que es el server de Kafka
- Para obtener más información de un topic, ejecutar:
  - `./kafka-topics.sh --bootstrap-server localhost:29092 --describe --topic my.new.topic`
- Cambiar el número de particiones que tiene un topic
  - `./kafka-topics.sh --bootstrap-server localhost:29092 --alter --topic my.new.topic --partitions 3`
- Eliminar el nuevo topic creado
  - `./kafka-topics.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --delete --topic my.new.topic`

### Consumer Group Tool

Es una colección de consumers que trabajan juntos consumiendo mensajes desde las particiones de un topic.

Para hacer un ejemplo de un grupo de consumers, necesitamos un topic con múltiples particiones.

- Creamos el topic `cg.demo.topic` con 5 particiones
  - `./kafka-topics.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --create --topic cg.demo.topic --partitions 5`
- Confirmamos que se ha creado correctamente en uno de los servers
  - `./kafka-topics.sh --bootstrap-server localhost:29092 --describe --topic cg.demo.topic`
- Los grupos de consumidores se crean automáticamente cuando un consumer se conecta a un topic o una partición
  - Se debe indicar el nombre del grupo de consumer al que queremos asociar ese consumer
  - Si no especificamos ningún grupo se asigna al por defecto
  - `./kafka-console-consumer.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --topic cg.demo.topic --group my.new.group`
- Ahora listamos en otra terminal la lista de grupos de consumers
  - `./kafka-consumer-groups.sh --bootstrap-server localhost:29092 --list`
- Obtenemos más información de nuestro grupo de consumers
  - `./kafka-consumer-groups.sh --bootstrap-server localhost:29092 --describe --group my.new.group`
- En otra terminal, arrancamos otro consumer
  - `./kafka-console-consumer.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --topic cg.demo.topic --group my.new.group`
- Ahora tenemos dos consumers en el mismo grupo y para el mismo topic
- Vemos como afecta esto al grupo de consumers
  - `./kafka-consumer-groups.sh --bootstrap-server localhost:29092 --describe --group my.new.group`
- En este ejemplo veo que el consumer1 aparece para las particiones 0, 1 y 2 y el consumer2 aparece para las particiones 3 y 4
  - Ocurre el proceso llamado rebalanceo
- Para obtener un estado de cada grupo de consumers
  - `./kafka-consumer-groups.sh --bootstrap-server localhost:29092 --describe --group my.new.group --state`
  - Se indica el número de miembros y el estado de salud, entre otras cosas
  - Importante para ver el estado de salud del grupo de consumers.
- Comprobar que consumers están en el grupo de consumers
  - `./kafka-consumer-groups.sh --bootstrap-server localhost:29092 --describe --group my.new.group --members`
- Para salir del grupo de consumers, en cada terminal donde tengo arrancado un consumer, pulsar `Ctrl+C`
- Para borrar un grupo de consumers
  - `./kafka-consumer-groups.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --delete --group my.new.group`
- Eliminamos el topic `cg.demo.topic`
  - `./kafka-topics.sh --bootstrap-server localhost:29092,localhost:39092,localhost:49092 --delete --topic cg.demo.topic`

## Coding Kafka with Spring Boot

El diseño de este sistema podría describirse como una arquitectura dirigida por eventos, que constaría de muchos microservicios independientes.

Cada uno de estos servicios puede consumir y/o producir múltiples tipos de eventos, accesos a BD relacionales o no relacionales...

El caso es que hay muchas posibilidades que se podrían estudiar, pero todo eso queda fuera del alcance del curso.

Nos centraremos en los elementos Kafka y Spring Boot de la implementación.

Vamos a tener un servicio de envío que reaccionará a las entradas y generará un evento cuando se envíe el pedido.

La comunicación entre producers y consumers se realiza a través de los topics en el broker. Este broker no se suele dibujar en los diagramas porque produce un ruido innecesario a la hora de entenderlos, pero la presencia del broker está implícita en los diagramas.

Un patrón útil para reducir la carga de un servicio en un sistema es dividir la actividad de consulta y transferirla a otro servicio (repeatable pattern).

Importante indicar que para este proyecto no se usa Zookeeper, ya deprecado, sino Kraft Cluster.

1. `dispatch`

Generamos el proyecto `dispatch` usando la web `https://start.spring.io/`, y usando como dependencias `Lombok` y `Spring for Apache Kafka`.
