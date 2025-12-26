# aosp-ipc-monitoring-client

Мониторинг IPC взаимодействий в android

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


## Пример отправляемого JSON

```json
{
  "device_id": "android_uuid_12345",
  "timestamp_sent": 1715420000123,
  "events": [
    {
      "type": "BROADCAST",
      "timestamp": 1715419999000,
      "source_pkg": "com.android.systemui",
      "target_pkg": "com.example.myapp",
      "payload": {
        "action": "android.intent.action.BATTERY_CHANGED",
        "extras": {"level": 85, "plugged": false}
      }
    },
    {
      "type": "SERVICE_BIND",
      "timestamp": 1715419999500,
      "source_pkg": "com.google.android.youtube",
      "target_pkg": "com.google.android.gms",
      "payload": {
        "service_class": "com.google.android.gms.auth.GetTokenService",
        "flags": "BIND_AUTO_CREATE"
      }
    },
    {
      "type": "PROVIDER_ACCESS",
      "timestamp": 1715419999800,
      "source_pkg": "com.instagram.android",
      "target_pkg": "com.android.providers.media",
      "payload": {
        "authority": "media",
        "uri": "content://media/external/images/media/123"
      }
    }
  ]
}
```

