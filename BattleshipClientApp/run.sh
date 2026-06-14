#!/bin/bash

clear

echo "===================================================="
echo " Запуск клиента Морского Боя..."
echo "===================================================="

if ! command -v mvn &> /dev/null
then
    echo "[ОШИБКА] Maven не найден! Установите его или добавьте в PATH."
    exit 1
fi

mvn -q compile exec:java -Dexec.mainClass="Main" -Dorg.slf4j.simpleLogger.defaultLogLevel=error

if [ $? -ne 0 ]; then
    echo ""
    echo "[ОШИБКА] Не удалось запустить приложение."
fi