package com.ugelaa.monitoreo.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

//Creamos el archivo físico donde se guardarán los datos
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sesion_docente")

class SessionManager(private val context: Context) {

    //Definimos las "Llaves" para cada dato que queremos guardar
    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val TOKEN = stringPreferencesKey("auth_token")
        val NOMBRE = stringPreferencesKey("user_nombre")
        val NICKNAME = stringPreferencesKey("user_nickname")
    }

    //Función para GUARDAR la sesión al hacer Login exitoso
    suspend fun guardarSesion(token: String, nombre: String, nickname: String) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[TOKEN] = token
            preferences[NOMBRE] = nombre
            preferences[NICKNAME] = nickname
        }
    }

    //Función para CERRAR sesión
    suspend fun limpiarSesion() {
        context.dataStore.edit { preferences ->
            preferences.clear() // Borra todo
        }
    }

    //Funciones para LEER si está logueado
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN] ?: false
    }

    val getNombre: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[NOMBRE] ?: ""
    }

    val getNickname: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[NICKNAME] ?: ""
    }
}