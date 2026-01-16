package com.example.vote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@SpringBootApplication
@RestController
public class VoteApplication {

  // Read Redis connection details from environment variables
  private final String redisHost =
      System.getenv().getOrDefault("REDIS_HOST", "localhost");

  private final int redisPort =
      Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

  private final String redisList =
      System.getenv().getOrDefault("REDIS_LIST", "votes");

  public static void main(String[] args) {
    SpringApplication.run(VoteApplication.class, args);
  }

  /**
   * Home page – voting UI
   */
  @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
public String home() {
  return """
    <!doctype html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>Earth vs Mars</title>
      <style>
        body {
          margin: 0;
          font-family: Arial, Helvetica, sans-serif;
          background-color: #f7f7f3; /* milk background */
          display: flex;
          justify-content: center;
          align-items: center;
          height: 100vh;
        }

        .container {
          max-width: 900px;
          width: 100%;
          padding: 20px;
          text-align: center;
        }

        h1 {
          margin-bottom: 30px;
          color: #333;
        }

        .cards {
          display: flex;
          gap: 30px;
          justify-content: center;
          flex-wrap: wrap;
        }

        .card {
          width: 280px;
          padding: 30px 20px;
          border-radius: 12px;
          box-shadow: 0 8px 20px rgba(0,0,0,0.1);
        }

        .earth {
          background: linear-gradient(135deg, #2ecc71, #3498db);
          color: white;
        }

        .mars {
          background: linear-gradient(135deg, #e74c3c, #e67e22);
          color: white;
        }

        .card h2 {
          margin-bottom: 20px;
        }

        button {
          background-color: rgba(255,255,255,0.95);
          border: none;
          border-radius: 8px;
          padding: 12px 24px;
          font-size: 16px;
          font-weight: bold;
          cursor: pointer;
        }

        button:hover {
          opacity: 0.9;
        }
      </style>
    </head>

    <body>
      <div class="container">
        <h1>Earth vs Mars — Swarm Update</h1>

        <div class="cards">
          <form method="post" action="/vote">
            <div class="card earth">
              <h2> Earth</h2>
              <button name="choice" value="earth">Vote Earth</button>
            </div>
          </form>

          <form method="post" action="/vote">
            <div class="card mars">
              <h2> Mars</h2>
              <button name="choice" value="mars">Vote Mars</button>
            </div>
          </form>
        </div>
      </div>
    </body>
    </html>
  """;
}


  /**
   * Handles vote submission
   */
  @PostMapping("/vote")
  public String vote(@RequestParam String choice) throws IOException {

    String normalized = choice.trim().toLowerCase();

    if (!normalized.equals("earth") && !normalized.equals("mars")) {
      return "Invalid vote";
    }

    // Push vote to Redis
    lpush(redisList, normalized);

    return """
      <html><body style="font-family: Arial; margin: 40px;">
        <h2>Vote recorded: %s</h2>
        <a href="/">Vote again</a>
      </body></html>
    """.formatted(normalized);
  }

  /**
   * Health endpoint (for containers later)
   */
  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of("status", "ok");
  }

  /**
   * Minimal Redis client using raw TCP + RESP protocol
   * LPUSH <list> <value>
   */
  private void lpush(String list, String value) throws IOException {

    try (
      Socket socket = new Socket(redisHost, redisPort);
      BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
      BufferedInputStream in = new BufferedInputStream(socket.getInputStream())
    ) {

      String command =
          "*3\r\n" +
          "$5\r\nLPUSH\r\n" +
          "$" + list.length() + "\r\n" + list + "\r\n" +
          "$" + value.length() + "\r\n" + value + "\r\n";

      out.write(command.getBytes(StandardCharsets.UTF_8));
      out.flush();

      // Read response (integer reply like :1)
      byte[] buffer = new byte[64];
      in.read(buffer);
    }
  }
}
