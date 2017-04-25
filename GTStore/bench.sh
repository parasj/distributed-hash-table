pkill -f Manager;
pkill -f DataNode;
pkill rmiregistry;

cd out/production/GTStore

rmiregistry &
echo "Started the registry"
sleep 1
java Manager &
echo "Started the Manager"
sleep 1

for i in $(seq 5 18)
do
    echo "Testing $i now"
    for j in $(seq 1 $i) 
    do
        echo "Starting DataNode $j"
        java DataNode 2>&1 >/dev/null &
    done;
    sleep 1
    java ShoppingCart > /tmp/results/$i.json;
    pkill -f DataNode;
    sleep 1
done;
pkill -f Manager;
pkill rmiregistry;
