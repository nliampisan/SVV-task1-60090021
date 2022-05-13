package th.ac.kmitl.se;

import org.graphwalker.core.machine.ExecutionContext;
import org.graphwalker.java.annotation.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

// Update the filename of the saved file of your model here.
@Model(file  = "model.json")
public class OrderAdapter extends ExecutionContext {
    // The following method add some delay between each step
    // so that we can see the progress in GraphWalker player.
    public static int delay = 0;
    @AfterElement
    public void afterEachStep() {
        try
        {
            Thread.sleep(delay);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    OrderDB orderDB;
    ProductDB productDB;
    PaymentService paymentService;
    ShippingService shippingService;
    Order order;
    Address address;
    Card card;
    float price = 1500f;
    float weight = 350f;
    int quantity = 2;
    String productID = "Apple Watch";
    String successCode = "success";
    String errorCode = "error";
    String trackingCode = "trackingCode";
    ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
    ArgumentCaptor<Float> amountCaptor = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<PaymentCallback> paymentCaptor  = ArgumentCaptor.forClass(PaymentCallback.class);
    ArgumentCaptor<String> confirmCodeCaptor = ArgumentCaptor.forClass(String.class);

    @BeforeExecution
    public void setUp() {
        // Add the code here to be executed before
        // GraphWalk starts traversing the model.
        address = new Address("address1", "line1", "line2", "district", "city", "1234");
        card = new Card("cardID", "name", 12,2022);
        orderDB = Mockito.mock(OrderDB.class);
        productDB = Mockito.mock(ProductDB.class);
        paymentService = Mockito.mock(PaymentService.class);
        shippingService = Mockito.mock(ShippingService.class);
        order = new Order(orderDB, productDB, paymentService, shippingService);

    }

    @Edge()
    public void reset() {
        System.out.println("Edge reset");
        order = new Order(orderDB, productDB, paymentService, shippingService);
    }

    @Edge()
    public void place() {
        System.out.println("Edge place");
        when(orderDB.getOrderID()).thenReturn(1);
        when(productDB.getPrice(productID)).thenReturn(price);
        when(productDB.getWeight(productID)).thenReturn(weight);

        order.place("John", productID,quantity, address);
        assertEquals(Order.Status.PLACED, order.getStatus());
    }

    @Edge()
    public void cancel() {
        System.out.println("Edge cancel");
        order.cancel();
        assertEquals(Order.Status.CANCELED, order.getStatus());
    }

    @Edge()
    public void pay() {
        System.out.println("Edge pay");
        when(orderDB.getOrderID()).thenReturn(1);
        when(productDB.getPrice(productID)).thenReturn(price);
        when(productDB.getWeight(productID)).thenReturn(weight);
        when(shippingService.getPrice(address,weight*quantity)).thenReturn(50f);

        order.pay(card);

        verify(paymentService).pay(cardCaptor.capture(),amountCaptor.capture(),paymentCaptor.capture());
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
        assertEquals(3050f, order.getTotalCost());
        assertEquals(3050f, amountCaptor.getValue());

    }

    @Edge()
    public void retryPay() {
        System.out.println("Edge retryPay");
        order.pay(card);
        paymentService.pay(cardCaptor.capture(),amountCaptor.capture(),paymentCaptor.capture());
        assertEquals(Order.Status.PAYMENT_CHECK, order.getStatus());
    }

    @Edge()
    public void paySuccess() {
        System.out.println("Edge paySuccess");
        paymentCaptor.getValue().onSuccess(successCode);
        assertEquals(Order.Status.PAID,order.getStatus());
        assertEquals(successCode,order.paymentConfirmCode);
    }

    @Edge()
    public void payError() {
        System.out.println("Edge payError");
        paymentCaptor.getValue().onError(errorCode);
        assertEquals(Order.Status.PAYMENT_ERROR,order.getStatus());
    }

    @Edge()
    public void ship() {
        System.out.println("Edge ship");
        when(shippingService.ship(any(Address.class),anyFloat())).thenReturn(trackingCode);
        order.ship();
        assertEquals(trackingCode, order.trackingCode);
        assertEquals(Order.Status.SHIPPED, order.getStatus());
    }

    @Edge()
    public void refund() {
        System.out.println("Edge refund");
        order.cancel();
        assertEquals(Order.Status.AWAIT_REFUND, order.getStatus());
        verify(paymentService).refund(confirmCodeCaptor.capture(),paymentCaptor.capture());
    }

    @Edge()
    public void refundSuccess() {
        System.out.println("Edge refundSuccess");
        paymentCaptor.getValue().onSuccess(successCode);
        assertEquals(Order.Status.REFUNDED,order.getStatus());
    }

    @Edge()
    public void refundError() {
        System.out.println("Edge refundError");
        paymentCaptor.getValue().onError(errorCode);
        assertEquals(Order.Status.REFUND_ERROR,order.getStatus());
    }
}
