# Reconcile and aggregate events using Kafka streams

This repository contains the code used for the following blog post:
https://www.hugomalves.com/reconcile-and-aggregate-events-using-kafka-streams

This Readme contains the instructions to run the application, and some quick introduction of the issue.
For more details about the application and the implementation please check the blog post.


# Intro

![Kstream flow](./diagrams/schema.png)

This demo shows how we can use Kafka Streams to combine and process incoming events from various sources. Once the aggregation process is complete and the consolidated data is prepared, a new, enriched event will be emitted to notify relevant consumers or systems.

To demonstrate the issue we will build a asynchronous order system for an online shopping company. The system enables the company to receive customer orders, manage the manufacturing process, and handle shipping, all while maintaining independent services and relying on event-based communication through Kafka topics.

Upon examining the previous diagram, it becomes clear that the shipping service must be aware of the order status before initiating the shipping process. An order must only be shipped after all products have been manufactured. However, this system lacks a central database to keep track of each order and its status. To address this issue, we will employ **Kafka Streams** to aggregate the event of all the involved topics and emit an enriched event when the order is ready to be shipped.

The **stateful aggregation** we will implement using Kafka Streams, allows the shipping service to know the current status of an order by subscribing to and processing the events associated with both the Order and Products Manufactured topics. By doing so, we can ensure that the shipping service only ships the order once all products within an order have been successfully manufactured. This approach eliminates the need for a central database to monitor the status of each order, as the Kafka Streams application will continuously process and update the order status based on the incoming events. Consequently, this will enable a more efficient and streamlined shipping process, as the shipping service will be automatically informed of the order status and can initiate the shipping process at the appropriate time.

The system is composed of 3 independent services:

* **Order service**. Receives the orders from the users and stores them in a Kafka topic.

* **Manufacturer service**. It subscribes to messages in the order topic. When it receives a new message, it will request the manufacturer of the products.

* **Shipping service**. After all items of the order are manufactured the shipping service can trigger the shipping phase. This service must be able to know the current order status. An order is considered ready to be shipped when all products are manufactured.


# How to run the application


Use docker to start the Kafka cluster:

```docker compose up -d```

AKHQ  is now accessible in the URL: http://localhost:9090/ui/docker-kafka-server/topic

## Test the reconciliation process

1. Run the Stream application:
 
    ```./gradlew runKStream```

    The KStream application will wait for new events.

1. **Create a new order by running the OrderProducer main.**

    ```./gradlew runOrderProducer```

    You should see on the logs a message similar to the following:
    ```
    Successfully sent new order to the orders topic: OrderDto[id='0af9bdce-3825-41a8-870e-1ed9a37a97ba', orderDate=2023-05-08T21:37:38.571850862, items=[ProductDto[id=1, name='Iphone'], ProductDto[id=2, name='Samsung']]]
    ```

    The previous order has the id 0af9bdce-3825-41a8-870e-1ed9a37a97ba, and contains 2 items. The streaming application will wait that the 2 items of the order are manufactured before sending the order to shipping.
   
1. **Check the logs of the Stream application**
    
    With the order already present in the Kafka "orders" topic, you should see the following logs in the stream application:
    ```  
    INFO  c.h.s.OrderManufacturingStatus - 0/2 manufactured products
    INFO  c.h.shipping.processor.BaseProcessor - Not all items of order 0af9bdce-3825-41a8-870e-1ed9a37a97ba were manufactured, waiting
    ```

    You can see on the previous log lines that the stream application detected a new order. This order contains 2 products that weren't yet manufactured. So the stream application is waiting for the products to be manufactured before emitting a new message to the shipping topic.    
  
1. **Send a message to the "products manufactured" topic containing the first item of the order.**

    ```./gradlew  runProductManufacturedProducer --args='4da816c2-3e4a-4a3c-94fb-3b60cdfa7618 1'```	

    **Replace the order id by the order Id generated before when you ran the orderProducer**

    On the KStream application logs you should see the information that 1 out 2 products were manufactured. However the KStream application is still waiting for the second one before shipping the order.

    ```
    INFO  c.h.s.OrderManufacturingStatus - 1/2 manufactured products
    INFO  c.h.shipping.processor.BaseProcessor - Not all items of order 4da816c2-3e4a-4a3c-94fb-3b60cdfa7618 were manufactured, waiting
    ```

1. **Send a message to the "products manufactured" topic containing the last item of the order.**

    ```./gradlew  runProductManufacturedProducer --args='4da816c2-3e4a-4a3c-94fb-3b60cdfa7618 2'```	

    With the last item of the order manufactured we the KStream application can now send the order for shipping. If we check the logs we see that was what happened. The KStream reconciliated all the information and detected that all items of the order were manufactured. So it emitted a new message to the shipping topic. 

    ```
    INFO  c.h.s.OrderManufacturingStatus - 2/2 manufactured products
    INFO  c.h.shipping.processor.BaseProcessor - All items of order 4da816c2-3e4a-4a3c-94fb-3b60cdfa7618 were manufactured, forwarding event to shipping
    ```

    If you check also the shipping topic on [AKHQ](http://localhost:9090/ui/docker-kafka-server/topic/shipping/data?sort=NEWEST&partition=All) you should see a new message in the shipping topic.


You can try to run this commands in a different order to see how the KStream application reacts to different scenarios. For example, you can try to send the message to the "products manufactured" topic before sending the order to the "orders" topic. You will see that the KStream will handle this case successfully. Due to the asynchronous nature of Kafka the KStream application was built taking into account that the messages can arrive in any order.

# Unit tests

The application contains unit tests for the KStream application. The tests are located in the **ShippingKstreamApplicationTest** class.
The tests are using the [TopologyTestDriver](https://kafka.apache.org/28/documentation/streams/developer-guide/testing.html) to test the KStream application.