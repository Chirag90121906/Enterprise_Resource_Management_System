# Enterprise-Resource-Management-System
A Java-based enterprise resource system with a Swing desktop interface and a local web app for employees, inventory, sales, and reports.

## Requirements

- JDK 11 or newer

## Run the desktop app

From the project root, compile the sources and launch the dashboard:

```sh
mkdir -p out
javac --add-modules jdk.httpserver -d out $(find src -name '*.java')
java -cp out Main
```

## Run the web app

Start the local web server with the same compiled classes:

```sh
java --add-modules jdk.httpserver -cp out Main --web
```

Open [http://127.0.0.1:8765](http://127.0.0.1:8765). The server listens on this computer only. Changes made in the website are saved to `employees.txt`, `inventory.txt`, and `sales.txt` in the project root.
