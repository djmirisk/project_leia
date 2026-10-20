package com.solaresenergy.sisfv.data.local

import com.solaresenergy.sisfv.data.local.entity.ContratoEntity
import com.solaresenergy.sisfv.data.local.entity.ProyectoEntity
import com.solaresenergy.sisfv.data.local.entity.SistemaSisfvEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoContratoEntity
import com.solaresenergy.sisfv.data.local.entity.TecnicoProyectoEntity

object DatosPruebaSeeder {

    suspend fun cargar(db: AppDatabase) {

        val proyectoDao = db.proyectoDao()
        val tecnicoProyectoDao = db.tecnicoProyectoDao()

        val contratoDao = db.contratoDao()
        val tecnicoContratoDao = db.tecnicoContratoDao()

        val sistemaDao = db.sistemaSisfvDao()

        // =========================================================
        // PROYECTOS
        // =========================================================

        val proyectos = listOf(

            ProyectoEntity(
                id = 1,
                codigoProyecto = "VAL-001",
                nombreProyecto = "Proyecto Solar Valledupar",
                municipio = "Valledupar",
                departamento = "Cesar"
            ),

            ProyectoEntity(
                id = 2,
                codigoProyecto = "GUA-001",
                nombreProyecto = "Proyecto Solar La Guajira",
                municipio = "Manaure",
                departamento = "La Guajira"
            ),

            ProyectoEntity(
                id = 3,
                codigoProyecto = "CES-001",
                nombreProyecto = "Proyecto Solar Cesar Norte",
                municipio = "Bosconia",
                departamento = "Cesar"
            )
        )

        proyectoDao.insertarOActualizarLista(proyectos)


        // =========================================================
        // ASIGNACIÓN DE TÉCNICOS A PROYECTOS
        // =========================================================

        val asignacionesProyectos = listOf(

            // Robinson
            TecnicoProyectoEntity(
                tecnicoId = 1,
                proyectoId = 1
            ),

            TecnicoProyectoEntity(
                tecnicoId = 1,
                proyectoId = 2
            ),

            // Coordinador
            TecnicoProyectoEntity(
                tecnicoId = 2,
                proyectoId = 3
            )
        )

        tecnicoProyectoDao.insertarOActualizarLista(asignacionesProyectos)


        // =========================================================
        // CONTRATOS
        // =========================================================

        val contratos = listOf(

            // Proyecto Valledupar
            ContratoEntity(
                id = 101,
                proyectoId = 1,
                codigoContrato = "CTR-VAL-001",
                nombreContrato = "Contrato Valledupar - Zona Norte"
            ),

            ContratoEntity(
                id = 102,
                proyectoId = 1,
                codigoContrato = "CTR-VAL-002",
                nombreContrato = "Contrato Valledupar - Zona Sur"
            ),

            // Proyecto La Guajira
            ContratoEntity(
                id = 201,
                proyectoId = 2,
                codigoContrato = "CTR-GUA-001",
                nombreContrato = "Contrato Manaure"
            ),

            // Proyecto Cesar Norte
            ContratoEntity(
                id = 301,
                proyectoId = 3,
                codigoContrato = "CTR-CES-001",
                nombreContrato = "Contrato Cesar Norte"
            )
        )

        contratoDao.insertarOActualizarLista(contratos)


        // =========================================================
        // ASIGNACIÓN DE TÉCNICOS A CONTRATOS
        // =========================================================

        val asignacionesContratos = listOf(

            // Robinson
            TecnicoContratoEntity(
                tecnicoId = 1,
                contratoId = 101
            ),

            TecnicoContratoEntity(
                tecnicoId = 1,
                contratoId = 102
            ),

            TecnicoContratoEntity(
                tecnicoId = 1,
                contratoId = 201
            ),

            // Coordinador
            TecnicoContratoEntity(
                tecnicoId = 2,
                contratoId = 301
            )
        )

        tecnicoContratoDao.insertarOActualizarLista(asignacionesContratos)


        // =========================================================
        // SISTEMAS SISFV
        // =========================================================

        val sistemas = listOf(

            // -----------------------------------------------------
            // CONTRATO CTR-VAL-001
            // -----------------------------------------------------

            SistemaSisfvEntity(
                id = 1,
                proyectoId = 1,
                contratoId = 101,
                codigoSisfv = "SISFV-001",
                documentoUsuario = "1001001001",
                nui = "NUI-VAL-001",
                nombreUsuario = "Juan Carlos Pérez",
                departamento = "Cesar",
                municipio = "Valledupar",
                comunidad = "Comunidad Norte",
                vereda = "La Mesa"
            ),

            SistemaSisfvEntity(
                id = 2,
                proyectoId = 1,
                contratoId = 101,
                codigoSisfv = "SISFV-002",
                documentoUsuario = "1001001002",
                nui = "NUI-VAL-002",
                nombreUsuario = "María Fernanda Gómez",
                departamento = "Cesar",
                municipio = "Valledupar",
                comunidad = "Comunidad Norte",
                vereda = "Patillal"
            ),

            // -----------------------------------------------------
            // CONTRATO CTR-VAL-002
            // -----------------------------------------------------

            SistemaSisfvEntity(
                id = 3,
                proyectoId = 1,
                contratoId = 102,
                codigoSisfv = "SISFV-003",
                documentoUsuario = "1001001003",
                nui = "NUI-VAL-003",
                nombreUsuario = "Pedro Antonio Rodríguez",
                departamento = "Cesar",
                municipio = "Valledupar",
                comunidad = "Comunidad Sur",
                vereda = "Atánquez"
            ),

            // -----------------------------------------------------
            // CONTRATO CTR-GUA-001
            // -----------------------------------------------------

            SistemaSisfvEntity(
                id = 4,
                proyectoId = 2,
                contratoId = 201,
                codigoSisfv = "SISFV-004",
                documentoUsuario = "1001001004",
                nui = "NUI-GUA-001",
                nombreUsuario = "Carlos Alberto Martínez",
                departamento = "La Guajira",
                municipio = "Manaure",
                comunidad = "Comunidad A",
                vereda = "Aremasain"
            ),

            SistemaSisfvEntity(
                id = 5,
                proyectoId = 2,
                contratoId = 201,
                codigoSisfv = "SISFV-005",
                documentoUsuario = "1001001005",
                nui = "NUI-GUA-002",
                nombreUsuario = "Ana María Torres",
                departamento = "La Guajira",
                municipio = "Manaure",
                comunidad = "Comunidad B",
                vereda = "Mayapo"
            ),

            // -----------------------------------------------------
            // CONTRATO CTR-CES-001
            // -----------------------------------------------------

            SistemaSisfvEntity(
                id = 6,
                proyectoId = 3,
                contratoId = 301,
                codigoSisfv = "SISFV-006",
                documentoUsuario = "1001001006",
                nui = "NUI-CES-001",
                nombreUsuario = "Luis Eduardo Hernández",
                departamento = "Cesar",
                municipio = "Bosconia",
                comunidad = "Comunidad Cesar Norte",
                vereda = "El Cruce"
            ),

            SistemaSisfvEntity(
                id = 7,
                proyectoId = 3,
                contratoId = 301,
                codigoSisfv = "SISFV-007",
                documentoUsuario = "1001001007",
                nui = "NUI-CES-002",
                nombreUsuario = "Sandra Milena Castro",
                departamento = "Cesar",
                municipio = "Bosconia",
                comunidad = "Comunidad Cesar Norte",
                vereda = "Loma Linda"
            )
        )

        sistemaDao.insertarOActualizarLista(sistemas)
    }
}