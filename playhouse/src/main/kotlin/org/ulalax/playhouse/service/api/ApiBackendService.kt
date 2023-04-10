package org.ulalax.playhouse.service.api

import org.ulalax.playhouse.service.SystemPanel

interface ApiBackendService {
    val systemPanel: SystemPanel
    val apiBaseSender: ApiBaseSender
}