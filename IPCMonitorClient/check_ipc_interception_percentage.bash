#!/bin/bash

# Значения по умолчанию
TRACE_DURATION_SECS=90
ATRACE_OUTPUT_FILE="atrace.output"
IPC_MONITOR_OUTPUT_FILE="jq_ipc_monitor_data.jsonl"
VERBOSE=false

# Функция отображения помощи
show_help() {
    cat << EOF
Использование: $0 [ОПЦИИ]

Опции:
  -h, --help              Показать эту справку
  -t, --trace-duration-secs СЕКУНДЫ
                          Длительность трассировки (по умолчанию: 90 секунд)
  -v, --verbose           Подробный вывод (логгирование полученных событий из системы мониторинга с событиями из Android (atrace))

Примеры:
  $0                                      # Запуск с параметрами по умолчанию (90 сек)
  $0 --trace-duration-secs 120                               # Трассировка на 120 секунд

EOF
}

# Парсинг параметров командной строки
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -t|--trace-duration-secs)
                if [[ -n "$2" && "$2" =~ ^[0-9]+$ ]]; then
                    TRACE_DURATION_SECS="$2"
                    shift 2
                else
                    echo "Ошибка: $1 требует числовое значение"
                    exit 1
                fi
                ;;
            --trace-duration-secs=*)
                # Поддержка формата --trace-duration-secs=120
                VALUE="${1#*=}"
                if [[ "$VALUE" =~ ^[0-9]+$ ]]; then
                    TRACE_DURATION_SECS="$VALUE"
                    shift
                else
                    echo "Ошибка: --trace-duration-secs требует числовое значение, получено: $VALUE"
                    exit 1
                fi
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            *)
                echo "Неизвестный параметр: $1"
                echo "Используйте $0 -h для получения справки"
                exit 1
                ;;
        esac
    done
}

# Парсим аргументы
parse_arguments "$@"

echo "Длительность трассировки: $TRACE_DURATION_SECS секунд"
echo "Запуск atrace"
adb shell atrace --async_start am aidl
echo "Ожидание $TRACE_DURATION_SECS секунд перед записью trace данных"
adb shell atrace --async_stop -o /data/local/tmp/atrace.output
echo "Данные из Android по IPC взаимодействиям (atrace) сохранены в файл atrace.output"
adb pull /data/local/tmp/atrace.output
timeout 2s adb logcat -s IpcMonitorReceiver > ipc_monitor_data.txt
cat ipc_monitor_data.txt | awk -F'jsonRaw = ' '{print $2}' > $IPC_MONITOR_OUTPUT_FILE
rm ipc_monitor_data.txt
echo "Данные системы мониторинга сохранены в $IPC_MONITOR_OUTPUT_FILE"

# Инициализируем переменные

CONTENT_PROVIDER_TOTAL_IPC_COUNT=0
CONTENT_PROVIDER_INTERCEPTED_IPC_COUNT=0

SERVICE_TOTAL_IPC_COUNT=0
SERVICE_INTERCEPTED_IPC_COUNT=0

BROADCAST_RECEIVER_TOTAL_IPC_COUNT=0
BROADCAST_RECEIVER_INTERCEPTED_IPC_COUNT=0

# Подсчет процента перехвата событий типа ContentProvider
for atrace_provider_record in $(grep -E "(query|insert|update|delete|call):" $ATRACE_OUTPUT_FILE | grep -v "call: settings" | awk -F'|' '{print $3}')
do
    provider_called_method=$(echo $atrace_provider_record | awk -F': ' '{print $1}')
    provider_authority=$(echo $atrace_provider_record | awk -F': ' '{print $2}')

    intercepted_ipc_monitor_provider_data=$(jq -Mrc --arg type "ContentProvider" --arg method "$provider_called_method" --arg authority "$provider_authority" 'select(.type == $type and .payload.method == $method and .payload.authority == $authority)' $IPC_MONITOR_OUTPUT_FILE)

    if [[ "$VERBOSE" == "true" ]]
    then
        echo "Через atrace в Android обнаружено событие об IPC с ContentProvider, provider_called_method = $provider_called_method, provider_authority = $provider_authority"
        jq_cmd="jq -Mrc --arg type \"ContentProvider\" --arg method \"$provider_called_method\" --arg authority \"$provider_authority\" 'select(.type == $type and .payload.method == $method and .payload.authority == $authority)' $IPC_MONITOR_OUTPUT_FILE"
        echo "Поиск перехваченных данных (intercepted_ipc_monitor_provider_data) системой мониторинга через jq_cmd = $jq_cmd"
        echo "intercepted_ipc_monitor_provider_data = $intercepted_ipc_monitor_provider_data"
    fi

    if [[ $intercepted_ipc_monitor_provider_data != "" ]]
    then
        CONTENT_PROVIDER_INTERCEPTED_IPC_COUNT=$((CONTENT_PROVIDER_INTERCEPTED_IPC_COUNT + 1))
    fi

    CONTENT_PROVIDER_TOTAL_IPC_COUNT=$((CONTENT_PROVIDER_TOTAL_IPC_COUNT + 1))
done

CONTENT_PROVIDER_IPC_INTERCEPTION_PERC=$(echo "scale=2; ($CONTENT_PROVIDER_TOTAL_IPC_COUNT * 100)/ $CONTENT_PROVIDER_TOTAL_IPC_COUNT" | bc)

# Подсчет процента перехвата событий типа Service
for atrace_service_record in $(grep -E "(startService|startForegroundService|bindService|unbindServiceLocked):" $ATRACE_OUTPUT_FILE | awk -F'|' '{print $3}')
do
    service_action=$(echo $atrace_service_record | awk -F':' '{print $1}')
    service_name=""
    case $service_action in
        startService|startForegroundService)
            service_name=$(echo $atrace_service_record | awk -F'cmp=' '{print $2}' | awk '{print $1}' | sed 's#\/##g')
            ;;
        bindService)
            service_name=$(echo $atrace_service_record | awk -F'/' '{print $2}' | awk -F'}' '{print $1}')
            ;;
        unbindServiceLocked)
            service_name=$(echo $atrace_service_record | awk '{print $2}')
            ;;
    esac

    intercepted_ipc_monitor_service_data=$(jq -Mrc --arg type "Service" --arg service_name "$service_name" --arg service_action "$service_action" 'select(.type == $type and .payload.service_name == $service_name and .payload.action == $service_action)' $IPC_MONITOR_OUTPUT_FILE)

    if [[ "$VERBOSE" == "true" ]]
    then
        echo "Через atrace в Android обнаружено событие об IPC с Service, service_action = $service_action, service_name = $service_name"
        jq_cmd="jq -Mrc --arg type \"Service\" --arg service_name \"$service_name\" --arg service_action \"$service_action\" 'select(.type == $type and .payload.service_name == $service_name and .payload.action == $service_action)' $IPC_MONITOR_OUTPUT_FILE"
        echo "Поиск перехваченных данных (intercepted_ipc_monitor_service_data) системой мониторинга через jq_cmd = $jq_cmd"
        echo "intercepted_ipc_monitor_service_data = $intercepted_ipc_monitor_service_data"
    fi
    
    if [[ $intercepted_ipc_monitor_service_data != "" ]]
    then
        SERVICE_INTERCEPTED_IPC_COUNT=$((SERVICE_INTERCEPTED_IPC_COUNT + 1))
    fi

    SERVICE_TOTAL_IPC_COUNT=$((SERVICE_TOTAL_IPC_COUNT + 1))
done

SERVICE_IPC_INTERCEPTION_PERC=$(echo "scale=2; ($SERVICE_INTERCEPTED_IPC_COUNT * 100)/ $SERVICE_TOTAL_IPC_COUNT" | bc)

# Подсчет процента перехвата событий типа BroadcastReceiver
grep "\|broadcastIntent" $ATRACE_OUTPUT_FILE | while read -r target_line && read -r intent_line; do
    # Извлекаем broadcastIntentTarget
    if [[ "$target_line" =~ broadcastIntentTarget:.* ]]; then
        broadcast_target=$(echo $target_line | awk -F'broadcastIntentTarget:' '{print $2}')
        if [[ "$broadcast_target" == "null" ]]
        then
            broadcast_target="*"
        fi
    fi
    
    # Извлекаем broadcastIntent
    if [[ "$intent_line" =~ broadcastIntent:.* ]]; then
        broadcast_action=$(echo $intent_line | awk -F'/' '{print $2}')
    fi
    
    intercepted_ipc_monitor_broadcast_data=$(jq -Mrc --arg type "BroadcastReceiver" --arg broadcast_target "$broadcast_target" --arg broadcast_action "$broadcast_action" 'select(.type == $type and .receiver == $broadcast_target and .payload.action == $broadcast_action)' $IPC_MONITOR_OUTPUT_FILE)

    if [[ "$VERBOSE" == "true" ]]
    then
        echo "Через atrace в Android обнаружено событие об IPC с BroadcastReceiver, broadcast_action = $broadcast_action, broadcast_target = $broadcast_target"
        jq_cmd="jq -Mrc --arg type \"BroadcastReceiver\" --arg broadcast_target \"$broadcast_target\" --arg broadcast_action \"$broadcast_action\" 'select(.type == $type and .receiver == $broadcast_target and .payload.action == $broadcast_action)' $IPC_MONITOR_OUTPUT_FILE"
        echo "Поиск перехваченных данных (intercepted_ipc_monitor_broadcast_data) системой мониторинга через jq_cmd = $jq_cmd"
        echo "intercepted_ipc_monitor_broadcast_data = $intercepted_ipc_monitor_broadcast_data"
    fi
    
    if [[ $intercepted_ipc_monitor_broadcast_data != "" ]]
    then
        BROADCAST_RECEIVER_INTERCEPTED_IPC_COUNT=$((BROADCAST_RECEIVER_INTERCEPTED_IPC_COUNT + 1))
    fi

    BROADCAST_RECEIVER_TOTAL_IPC_COUNT=$((BROADCAST_RECEIVER_TOTAL_IPC_COUNT + 1))
done

BROADCAST_RECEIVER_IPC_INTERCEPTION_PERC=$(echo "scale=2; ($BROADCAST_RECEIVER_INTERCEPTED_IPC_COUNT * 100)/ $BROADCAST_RECEIVER_TOTAL_IPC_COUNT" | bc)

echo "Результаты проверки с подсчетом процента перехвата IPC в разбивке по типу:"
echo "ContentProvider: ${CONTENT_PROVIDER_IPC_INTERCEPTION_PERC}%"
echo "Service: ${SERVICE_IPC_INTERCEPTION_PERC}%"
echo "BroadcastReceiver: ${CONTENT_PROVIDER_IPC_INTERCEPTION_PERC}%"