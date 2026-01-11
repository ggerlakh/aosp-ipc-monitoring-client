# aosp-ipc-monitoring-client

Мониторинг IPC взаимодействий в android

* `IPCMonitorClient` - Android Studio проект клиенсткого приложения на kotlin для получения данных об IPC взаимодействиях от Android
* `aosp` - Патч исходного кода ОС android (AOSP) для получения данных об IPC взаимодействиях

## Извлечение данных

Предполагается вести мониторинг за тремя основными механизмами IPC взаимодействий в Android: 

| Механизм | Назначение в Примере | Направление Связи | Основная Функция |
|----------|---------------------|------------------|------------------|
| ContentProvider | Компонент, который управляет доступом к структурированному набору данных. Это стандартизированный и безопасный способ обмена данными между приложениями, даже если они работают в разных процессах. | Двусторонняя (запрос-ответ) | Безопасный доступ к базе данных другого приложения |
| Service (Bound) | Компонент, который может выполнять длительные операции в фоновом режиме, не имея пользовательского интерфейса. Службы являются основой для более сложных форм IPC в Android. | Двусторонняя (устойчивый вызов методов) | Выполнение методов в другом процессе и получение Live-данных |
| BroadcastReceiver | Компонент, который позволяет приложению получать широковещательные сообщения от системы или других приложений, а также отправлять свои собственные сообщения. Это асинхронный, односторонний механизм. | Односторонняя (издатель-подписчик) | Реакция на события между процессами |


Команда `adb shell dumpsys` выводит информацию о текущем состоянии всех системных сервисов.  

Основная информация для мониторинга IPC взаимодействий (Service, ContentProvider, BroadcastRecevier) находится в **ActivityManagerService**.  
Чтобы посмотреть информацию о нем, нужно выполнить команду:
```bash
adb shell dumpsys activity
```
Пример вывода данных этой команды приведен в файле `adb_shell_activity_log.txt`.  

Для того чтобы получить информацию по конкретному механизму IPC взаимодействий в системе, нужно выполнить соответствующую команду:
```bash
adb shell dumpsys activity broadcasts
adb shell dumpsys activity providers
adb shell dumpsys activity services
```

Пример вывода данных также есть в файле `adb_shell_activity_log.txt`.  


## Пример получаемых из AOSP данных в формате JSON


```json
{
    "type": "ContentProvider",
    "sender": "com.android.phone",
    "receiver": "com.android.providers.telephony",
    "payload": {
        "authority": "telephony",
        "uri": "content://telephony/siminfo/1",
        "method": "update"
    },
    "timestamp": 1768083376804
},
{
    "type": "Service",
    "sender": "android",
    "receiver": "com.android.providers.calendar",
    "payload": {
        "action": "bindService",
        "service_type": "BoundService"
    },
    "timestamp": 1768083381084
},
{
    "type": "BroadcastReceiver",
    "sender": "android",
    "receiver": "*",
    "payload": {
        "action": "android.net.conn.DATA_ACTIVITY_CHANGE"
    },
    "timestamp": 1768156960530
}
```

