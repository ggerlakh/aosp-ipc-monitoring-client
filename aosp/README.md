# Патч исходного кода ОС android (AOSP) для получения данных об IPC взаимодействиях

В данной директории приведены только те файлы, которые были подвержены изменению из всего кода AOSP, c сохранением соответтствующих отсносительных путей в [исходном коде проекта](https://cs.android.com/android/platform/superproject/main/+/main:).  
Изменения в коде обрамляются соответствующими коментариями
```
// START CUSTOM IPC MONITOR
...
// END CUSTOM IPC MONITOR
```

##  Инструкция по сборке AOSP и локальном запуске собранного образа ОС в Android Studio на macbook air M2

Вся полезная информация про андроид разаботку есть на https://source.android.com/docs/setup?hl=ru 

Требуется установка IDE Android Studio (на том же сайте можно установить)

Полезная команда для дебага на эмуляторе 
```bash
# Просмотр всех логов в ОС
adb shell logcat
# Просмотр логов на стороне ОС связанных с отправкой данных об IPC взаиодействиях клиенсткому приложению
adb logcat -s IPC_MONITOR
# Просмотр клиентских логов связанных с обработкой данных об IPC от ОС
adb logcat -s IpcMonitorReceiver
```


1. Если на машине/сервере/ноутбуке еще не скачаны исходники aosp, его нужно скачать следуя вот этой инструкции https://source.android.com/docs/setup/download?hl=ru 
2. <*Перед этим шагом предполагается внесение изменений в исходный код aosp для создания и тестирования "своей" сборки*> Следующим шагом, нужно будет собрать образ Android под указанный "таргет" (с учетом архитектуры целевого устройства на котором предполагается осуществлять запуск, в случае с macbook M2 это будет arm64)
   ```bash
   cd aosp # или другая папка где находится скачанный исходный код aosp
   source build/envsetup.sh
   # опционально, указать другую директорию с артикфатами сборки aosp (по умолчанию "out")
   export OUT_DIR="out_05012026_cp_patch"
   lunch sdk_phone64_arm64-aosp_current-userdebug # сборка под arm64
   m
   # для генериации zip архива с avd для android studio
   make emu_img_zip
   # результат сборки в виде архива сохраняется по этому пути
   ls -l aosp/out_arm/target/product/emu64a/sdk-repo-linux-system-images.zip
   ```
3. Далее нужно скопировать собранный архив к себе локально (например через scp) и положить его по нужному пути в system-images для android SDK
   
   Предварительно рекомендуется себе экспортировать следующие переменные в .zshrc/.bashrc
   ```bash
   # переменные для работы android studio и android emulator
   export ANDROID_HOME=/Users/ggerlakh/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/emulator
   source ~/.zshrc
   # команда для проверки
   emulator -list-avds
   ```

   ```bash
   # пример команды scp
   scp -i ~/.ssh/id_rsa_ipc_monitoring_vm <login>@<ip>:/home/<login>/aosp/out_arm/target/product/emu64a/sdk-repo-linux-system-images.zip out_arm
   # после распаковки появится директория arm64-v8a
   #cd out_arm
   #unzip sdk-repo-linux-system-images.zip
   # создаем директорию с названием нашего кастомного system-image (название default, так как оно указано в файле out_arm/arm64-v8a/source.properties в поле SystemImage.TagId)
   #mkdir -p $ANDROID_HOME/system-images/android-36/default
   #cp -r arm64-v8a $ANDROID_HOME/system-images/android-36/default
   
   # Удаляем старую версию
   rm -rf $ANDROID_HOME/system-images/android-36/default/arm64-v8a
   # Разархивируем новую 
   unzip out_05012026_cp_patch/sdk-repo-linux-system-images.zip -d $ANDROID_HOME/system-images/android-36/default
   ```
   
4. Затем нужно создать свой android virtual device (AVD), указав в нем свой только что собранный system-image ОС android
   
   Для этого сначала нужно создать директорию для нового AVD с названием `my-custom-avd` 
   ```bash
    mkdir -p ~/.android/avd/my-custom-avd.avd
   ```
   
   Затем создать два файла с описанием и конфгурацией для нового AVD
   ```bash
    cat ~/.android/avd/my-custom-avd.ini
	avd.ini.encoding=UTF-8
	path=$HOME/.android/avd/my-custom-avd.avd
	path.rel=avd/my-custom-avd.avd
	target=android-36
	
    cat ~/.android/avd/my-custom-avd.avd/config.ini 
	avd.ini.encoding = UTF-8
	avd.ini.displayname = my-custom-avd
	abi.type = arm64-v8a
	hw.cpu.arch = arm64
	hw.cpu.ncore = 4
	hw.device.name = pixel_8
	hw.ramSize = 4096
	hw.lcd.width = 1080
	hw.lcd.height = 2340
	hw.lcd.density = 420
	image.sysdir.1 = system-images/android-36/default/arm64-v8a/ 
	target = android-36
	tag.id = default
	disk.dataPartition.size = 6442450944
   ```

4. После этого, созданное устройство должно быть доступно для запуска 
5. Ручной запуск приложения в эмуляторе (так как android studio может не захотеть распознавать созданный avd и system-image)
   ```bash
    # для сборки APK из kotlin-проекта
    ./gradlew assembleDebug
    # для запуска эмулятора
    emulator -avd custom-aosp-client
    # для установки собранной APK в установленный AVD
    adb install -r app/build/outputs/apk/debug/app-debug.apk 
   ```
6. Для пересборки изменений в `frameworks/base/core/java/android/content/ContentProvider.java` можно использовать команду `make framework-minus-apex` вместо пересборки всего проекта