using System.Net.Sockets;
using System.Text;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Npgsql;

var builder = Host.CreateApplicationBuilder(args);
builder.Services.AddHostedService<Worker>();
var host = builder.Build();
host.Run();

public class Worker : BackgroundService
{
    private readonly ILogger<Worker> _logger;

    // Redis config
    private readonly string _redisHost = Env("REDIS_HOST", "localhost");
    private readonly int _redisPort = int.Parse(Env("REDIS_PORT", "6379"));
    private readonly string _redisList = Env("REDIS_LIST", "votes");

    // Postgres config
    private readonly string _dbHost = Env("DB_HOST", "localhost");
    private readonly int _dbPort = int.Parse(Env("DB_PORT", "5432"));
    private readonly string _dbUser = Env("DB_USER", "earthmars");
    private readonly string _dbPassword = Env("DB_PASSWORD", "earthmars_password");
    private readonly string _dbName = Env("DB_NAME", "earthmarsdb");

    public Worker(ILogger<Worker> logger)
    {
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        _logger.LogInformation("Worker started");

        var connString =
            $"Host={_dbHost};Port={_dbPort};Username={_dbUser};Password={_dbPassword};Database={_dbName}";

        // Wait for database to be ready
        await WaitForDatabase(connString, stoppingToken);

        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                var vote = await BrpopAsync(_redisList, stoppingToken);

                if (vote is null)
                    continue;

                await InsertVote(connString, vote, stoppingToken);
                _logger.LogInformation("Processed vote: {Vote}", vote);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Worker error, retrying...");
                await Task.Delay(2000, stoppingToken);
            }
        }
    }

    // ---- Helpers ----

    private static string Env(string key, string fallback) =>
        Environment.GetEnvironmentVariable(key) ?? fallback;

    private async Task WaitForDatabase(string connString, CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            try
            {
                await using var conn = new NpgsqlConnection(connString);
                await conn.OpenAsync(ct);
                await using var cmd = new NpgsqlCommand("SELECT 1;", conn);
                await cmd.ExecuteScalarAsync(ct);
                _logger.LogInformation("Database is ready");
                return;
            }
            catch
            {
                _logger.LogInformation("Waiting for database...");
                await Task.Delay(2000, ct);
            }
        }
    }

    private async Task InsertVote(string connString, string vote, CancellationToken ct)
    {
        await using var conn = new NpgsqlConnection(connString);
        await conn.OpenAsync(ct);

        const string sql = "INSERT INTO votes(choice) VALUES (@choice)";
        await using var cmd = new NpgsqlCommand(sql, conn);
        cmd.Parameters.AddWithValue("choice", vote);
        await cmd.ExecuteNonQueryAsync(ct);
    }

    // Redis BRPOP (blocking read)
    private async Task<string?> BrpopAsync(string list, CancellationToken ct)
    {
        using var client = new TcpClient();
        await client.ConnectAsync(_redisHost, _redisPort, ct);
        using var stream = client.GetStream();

        var cmd =
            $"*3\r\n$5\r\nBRPOP\r\n${list.Length}\r\n{list}\r\n$1\r\n0\r\n";

        var bytes = Encoding.UTF8.GetBytes(cmd);
        await stream.WriteAsync(bytes, ct);
        await stream.FlushAsync(ct);

        using var reader = new StreamReader(stream, Encoding.UTF8);

        // RESP parsing
        await reader.ReadLineAsync(ct); // *2
        await reader.ReadLineAsync(ct); // $len
        await reader.ReadLineAsync(ct); // list name
        await reader.ReadLineAsync(ct); // $len
        return await reader.ReadLineAsync(ct); // vote value
    }
}
