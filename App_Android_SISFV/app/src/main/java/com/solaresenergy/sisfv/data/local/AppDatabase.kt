package com.solaresenergy.sisfv.data.local

import com.solaresenergy.sisfv.data.local.dao.MantenimientoActividadDao
import com.solaresenergy.sisfv.data.local.dao.MantenimientoMedidorDao
import com.solaresenergy.sisfv.data.local.dao.ProyectoDao
import com.solaresenergy.sisfv.data.local.dao.SistemaSisfvDao
import com.solaresenergy.sisfv.data.local.dao.TecnicoProyectoDao
import com.solaresenergy.sisfv.data.local.dao.TecnicoDao
import com.solaresenergy.sisfv.data.local.dao.ContratoDao
import com.solaresenergy.sisfv.data.local.dao.TecnicoContratoDao
import com.solaresenergy.sisfv.data.local.dao.MantenimientoDao
import com.solaresenergy.sisfv.data.local.dao.MantenimientoFotoDao
import com.solaresenergy.sisfv.data.local.entity.MantenimientoFotoEntity
import com.solaresenergy.sisfv.data.local.entity.MantenimientoEntity
import com.solaresenergy.sisfv.data.local.entity.ProyectoEntity
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoProyectoEntity
import com.solaresenergy.sisfv.data.local.entity.ContratoEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoContratoEntity
import com.solaresenergy.sisfv.data.local.entity.MantenimientoActividadEntity
import com.solaresenergy.sisfv.data.local.entity.MantenimientoMedidorEntity
import com.solaresenergy.sisfv.data.local.dao.SolicitudCambioContactoDao
import com.solaresenergy.sisfv.data.local.dao.SolicitudCambioTitularidadDao
import com.solaresenergy.sisfv.data.local.entity.SolicitudCambioContactoEntity
import com.solaresenergy.sisfv.data.local.entity.SolicitudCambioTitularidadEntity
import com.solaresenergy.sisfv.data.local.dao.CambioComponenteDao
import com.solaresenergy.sisfv.data.local.entity.CambioComponenteEntity
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch




@Database(
    entities = [
        TecnicoEntity::class,
        MantenimientoEntity::class,
        ProyectoEntity::class,
        TecnicoProyectoEntity::class,
        SistemaSisfvEntity::class,
        ContratoEntity::class,
        TecnicoContratoEntity::class,
        MantenimientoActividadEntity::class,
        MantenimientoMedidorEntity::class,
        MantenimientoFotoEntity::class,
        SolicitudCambioContactoEntity::class,
        SolicitudCambioTitularidadEntity::class,
        CambioComponenteEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tecnicoDao(): TecnicoDao
    abstract fun mantenimientoDao(): MantenimientoDao
    abstract fun proyectoDao(): ProyectoDao
    abstract fun tecnicoProyectoDao(): TecnicoProyectoDao
    abstract fun sistemaSisfvDao(): SistemaSisfvDao
    abstract fun contratoDao(): ContratoDao
    abstract fun tecnicoContratoDao(): TecnicoContratoDao

    abstract fun mantenimientoActividadDao(): MantenimientoActividadDao

    abstract fun mantenimientoMedidorDao(): MantenimientoMedidorDao

    abstract fun mantenimientoFotoDao(): MantenimientoFotoDao

    abstract fun solicitudCambioContactoDao(): SolicitudCambioContactoDao

    abstract fun solicitudCambioTitularidadDao(): SolicitudCambioTitularidadDao

    abstract fun cambioComponenteDao(): CambioComponenteDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migración de Room versión 3 → 4.
         *
         * Se agregan las tablas:
         * - contratos
         * - tecnicos_contratos
         *
         * Se agregan los campos:
         * - contratoId
         * - comunidad
         *
         * No se eliminan los datos existentes.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {

                // Crear tabla de contratos
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS `contratos` (
                    `id` INTEGER NOT NULL,
                    `proyectoId` INTEGER NOT NULL,
                    `codigoContrato` TEXT NOT NULL,
                    `nombreContrato` TEXT NOT NULL,
                    `activo` INTEGER NOT NULL,
                    `ultimaSincronizacion` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent()
                )

                // Crear tabla de asignación técnico - contrato
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS `tecnicos_contratos` (
                    `tecnicoId` INTEGER NOT NULL,
                    `contratoId` INTEGER NOT NULL,
                    `activo` INTEGER NOT NULL,
                    `fechaAsignacion` INTEGER NOT NULL,
                    PRIMARY KEY(`tecnicoId`, `contratoId`)
                )
            """.trimIndent()
                )

                // Agregar contratoId a SISFV existentes
                db.execSQL(
                    """
                ALTER TABLE `sistemas_sisfv`
                ADD COLUMN `contratoId` INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
                )

                // Agregar comunidad a SISFV existentes
                db.execSQL(
                    """
                ALTER TABLE `sistemas_sisfv`
                ADD COLUMN `comunidad` TEXT
            """.trimIndent()
                )
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {

            override fun migrate(db: SupportSQLiteDatabase) {

                // =====================================================
                // ACTIVIDADES DE MANTENIMIENTO
                // =====================================================

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `mantenimiento_actividades` (
                `id` TEXT NOT NULL,
                `mantenimientoId` TEXT NOT NULL,
                `codigoActividad` TEXT NOT NULL,
                `realizada` INTEGER NOT NULL,
                `resultado` TEXT,
                `observacion` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())

                // =====================================================
                // MEDIDOR DE ENERGÍA
                // =====================================================

                db.execSQL("""
            CREATE TABLE IF NOT EXISTS `mantenimiento_medidor` (
                `mantenimientoId` TEXT NOT NULL,
                `aplica` INTEGER NOT NULL,
                `estado` TEXT,
                `codigoActivo` TEXT,
                PRIMARY KEY(`mantenimientoId`)
            )
        """.trimIndent())
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
            ALTER TABLE `mantenimiento_medidor`
            ADD COLUMN `energiaConsumidaKwh` REAL
        """.trimIndent())

                db.execSQL("""
            ALTER TABLE `mantenimiento_medidor`
            ADD COLUMN `evidenciaLecturaPath` TEXT
        """.trimIndent())

                db.execSQL("""
            ALTER TABLE `mantenimiento_medidor`
            ADD COLUMN `evidenciaCodigoPath` TEXT
        """.trimIndent())

                db.execSQL("""
            ALTER TABLE `mantenimiento_medidor`
            ADD COLUMN `fechaHoraLectura` INTEGER
        """.trimIndent())
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {

            override fun migrate(db: SupportSQLiteDatabase) {

                // Estado final de la visita
                db.execSQL(
                    """
            ALTER TABLE `mantenimientos_offline`
            ADD COLUMN `estadoFinalSisfv` TEXT
            """.trimIndent()
                )

                db.execSQL(
                    """
            ALTER TABLE `mantenimientos_offline`
            ADD COLUMN `observacionEstadoFinal` TEXT
            """.trimIndent()
                )

                db.execSQL(
                    """
            ALTER TABLE `mantenimientos_offline`
            ADD COLUMN `causaNoFuncional` TEXT
            """.trimIndent()
                )

                // Datos de la persona que recibe el mantenimiento
                db.execSQL(
                    """
            ALTER TABLE `mantenimientos_offline`
            ADD COLUMN `nombreReceptorFirma` TEXT
            """.trimIndent()
                )

                db.execSQL(
                    """
            ALTER TABLE `mantenimientos_offline`
            ADD COLUMN `documentoReceptorFirma` TEXT
            """.trimIndent()
                )

                db.execSQL(
                    """
            ALTER TABLE `mantenimientos_offline`
            ADD COLUMN `receptorEsTitular` INTEGER
            """.trimIndent()
                )

                // Estado del bombillo
                db.execSQL(
                    """
            ALTER TABLE `mantenimientos_offline`
            ADD COLUMN `bombilloEncendido` INTEGER
            """.trimIndent()
                )

                // Tabla de evidencias fotográficas
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `mantenimiento_fotos` (
                `mantenimientoId` TEXT NOT NULL,
                `codigoFoto` TEXT NOT NULL,
                `orden` INTEGER NOT NULL,
                `titulo` TEXT NOT NULL,
                `rutaOriginal` TEXT NOT NULL,
                `rutaConMarcaAgua` TEXT,
                `fechaCaptura` INTEGER NOT NULL,
                `estadoSync` TEXT NOT NULL,
                PRIMARY KEY(`mantenimientoId`, `codigoFoto`)
            )
            """.trimIndent()
                )
            }
        }
             private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mantenimiento_fotos` ADD COLUMN `latitudGps` REAL")
                db.execSQL("ALTER TABLE `mantenimiento_fotos` ADD COLUMN `longitudGps` REAL")
                db.execSQL("ALTER TABLE `mantenimiento_fotos` ADD COLUMN `precisionGps` REAL")
                db.execSQL("ALTER TABLE `mantenimiento_fotos` ADD COLUMN `hashSha256` TEXT")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `solicitudes_cambio_contacto` (
                `id` TEXT NOT NULL,
                `sistemaId` INTEGER NOT NULL,
                `codigoSisfv` TEXT NOT NULL,
                `nombreUsuario` TEXT NOT NULL,
                `documentoUsuario` TEXT,
                `tecnicoId` INTEGER NOT NULL,
                `telefonoPropuesto` TEXT NOT NULL,
                `correoPropuesto` TEXT,
                `fechaRegistro` INTEGER NOT NULL,
                `estadoRevision` TEXT NOT NULL,
                `estadoSync` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
                )
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `solicitudes_cambio_titularidad` (
                        `id` TEXT NOT NULL,
                        `sistemaId` INTEGER NOT NULL,
                        `codigoSisfv` TEXT NOT NULL,
                        `contratoId` INTEGER NOT NULL,
                        `proyectoId` INTEGER NOT NULL,
                        `nombreUsuarioActual` TEXT NOT NULL,
                        `documentoUsuarioActual` TEXT,
                        `nombreUsuarioNuevo` TEXT NOT NULL,
                        `documentoUsuarioNuevo` TEXT NOT NULL,
                        `telefonoUsuarioNuevo` TEXT NOT NULL,
                        `correoUsuarioNuevo` TEXT,
                        `motivo` TEXT NOT NULL,
                        `fotoDocumentoFrentePath` TEXT NOT NULL,
                        `fotoDocumentoReversoPath` TEXT NOT NULL,
                        `fotoHuellaPath` TEXT NOT NULL,
                        `firmaTecnicoPath` TEXT NOT NULL,
                        `firmaNuevoUsuarioPath` TEXT NOT NULL,
                        `tecnicoId` INTEGER NOT NULL,
                        `fechaRegistro` INTEGER NOT NULL,
                        `estadoRevision` TEXT NOT NULL,
                        `estadoSync` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cambios_componente` (
                        `id` TEXT NOT NULL,
                        `sistemaId` INTEGER NOT NULL,
                        `codigoSisfv` TEXT NOT NULL,
                        `contratoId` INTEGER NOT NULL,
                        `proyectoId` INTEGER NOT NULL,
                        `nombreUsuario` TEXT NOT NULL,
                        `documentoUsuario` TEXT,
                        `componente` TEXT NOT NULL,
                        `causaFallo` TEXT NOT NULL,
                        `fotoComponenteMaloPath` TEXT NOT NULL,
                        `fotoSerialMaloPath` TEXT NOT NULL,
                        `fotoComponenteNuevoPath` TEXT NOT NULL,
                        `fotoSerialNuevoPath` TEXT NOT NULL,
                        `nombreReceptor` TEXT NOT NULL,
                        `documentoReceptor` TEXT NOT NULL,
                        `receptorEsTitular` INTEGER NOT NULL,
                        `firmaUsuarioPath` TEXT NOT NULL,
                        `firmaTecnicoPath` TEXT NOT NULL,
                        `tecnicoId` INTEGER NOT NULL,
                        `fechaRegistro` INTEGER NOT NULL,
                        `estadoRevision` TEXT NOT NULL,
                        `estadoSync` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `cambios_componente` ADD COLUMN `fotoSegundoComponenteMaloPath` TEXT")
                db.execSQL("ALTER TABLE `cambios_componente` ADD COLUMN `fotoSegundoSerialMaloPath` TEXT")
                db.execSQL("ALTER TABLE `cambios_componente` ADD COLUMN `fotoSegundoComponenteNuevoPath` TEXT")
                db.execSQL("ALTER TABLE `cambios_componente` ADD COLUMN `fotoSegundoSerialNuevoPath` TEXT")
            }
        }
        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sisfv_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .addCallback(DatabaseCallback())
                    .build()

                INSTANCE = instance

                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                INSTANCE?.let { database ->

                    CoroutineScope(Dispatchers.IO).launch {

                        // Cargar técnicos de prueba
                        populateTestTechnicians(
                            database.tecnicoDao()
                        )

                        // Cargar proyectos, contratos,
                        // asignaciones y SISFV de prueba
                        DatosPruebaSeeder.cargar(database)
                    }
                }
            }

            suspend fun populateTestTechnicians(
                tecnicoDao: TecnicoDao
            ) {

                val tecnicosPrueba = listOf(

                    TecnicoEntity(
                        id = 1,
                        documento = "123456789",
                        nombre = "Robinson Puentes (Técnico de Campo)",
                        email = "r.puentes@gvscol.com",
                        passwordHash = "1234",
                        rol = "TECNICO"
                    ),

                    TecnicoEntity(
                        id = 2,
                        documento = "987654321",
                        nombre = "Coordinador de Operaciones",
                        email = "operaciones@solaresenergy.com",
                        passwordHash = "1234",
                        rol = "COORDINADOR"
                    )
                )

                tecnicoDao.insertarOActualizarLista(
                    tecnicosPrueba
                )
            }
        }
    }
}
