package siak

import zio.*

// Konfigurasi dibaca dari environment variable.
final case class AppConfig(jwtSecret: Config.Secret, databasePath: String, accessTokenExpireMinutes: Int)

object AppConfig:
  val config: Config[AppConfig] =
    (Config.secret("JWT_SECRET") ++
      Config.string("DATABASE_PATH").withDefault("../db/siak.db") ++
      Config.int("ACCESS_TOKEN_EXPIRE_MINUTES").withDefault(30)).map(AppConfig.apply)

  val layer: ZLayer[Any, Config.Error, AppConfig] = ZLayer.fromZIO(ZIO.config(config))
