# opentracing-quarkus
Demo for using opentracing/jaeger with quarkus

# [documentation here](https://guhilling.github.io/opentracing-quarkus/)


#1 - To START the database and jaeger

go in the root and run this command in a terminal

docker-compose -f .\docker-compose-dep.yml up

#2 - start the 2 applications

Open a terminal for each applications

cd quarkus-hello
mvn quarkus:dev "-Ddebug=5006"

cd quarkus-world
mvn quarkus:dev 

#3 - send a command to this the workflow

curl http://localhost:8080/hello/world


#4 - open jaeger

http://localhost:16686/



