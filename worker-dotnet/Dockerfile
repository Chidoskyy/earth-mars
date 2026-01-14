# -------- BUILD STAGE --------
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src

COPY Worker.csproj .
RUN dotnet restore

COPY . .
RUN dotnet publish -c Release -o /out

# -------- RUNTIME STAGE --------
FROM mcr.microsoft.com/dotnet/runtime:8.0
WORKDIR /app

COPY --from=build /out .

ENTRYPOINT ["dotnet", "Worker.dll"]
