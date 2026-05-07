package com.jdw.skillstestapp.repository

import com.jdw.skillstestapp.data.ChatMessageDao
import com.jdw.skillstestapp.data.model.ChatMessage
import com.jdw.skillstestapp.data.model.weather.WeatherData
import com.jdw.skillstestapp.data.network.WeatherApi
import javax.inject.Inject

class MyAppRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val weatherApi: WeatherApi,
) {
    suspend fun getAllMessage(): List<ChatMessage> = chatMessageDao.getAllMessage()

    suspend fun insertMessage(chatMessage: ChatMessage) = chatMessageDao.insertMessage(chatMessage)

    suspend fun deleteMessage(chatMessage: ChatMessage) = chatMessageDao.deleteMessage(chatMessage)

    suspend fun getWeather(lat: Double, lon: Double): WeatherData =
        weatherApi.getWeather(lat.toString(), lon.toString())
}
