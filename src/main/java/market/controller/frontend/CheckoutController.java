package market.controller.frontend;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import market.domain.Cart;
import market.domain.CartItem;
import market.domain.Order;
import market.domain.Product;
import market.domain.dto.ContactsDTO;
import market.domain.dto.CreditCardDTO;
import market.exception.EmptyCartException;
import market.service.CartService;
import market.service.ContactsService;
import market.service.OrderService;
import market.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Контроллер оформления заказа.
 */
@Controller
@RequestMapping("/checkout")
@Secured({"ROLE_USER"})
@SessionAttributes({"cart"})
public class CheckoutController {

    @Value("${deliveryCost}")
    private int deliveryCost;
    
    @Autowired
    private UserAccountService userAccountService;
    
    @Autowired
    private ContactsService contactsService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CartService cartService;

    //--------------------------------------------- Изменение контактных данных
    
    /**
     * Страница изменения данных.
     *
     * @param principal
     * @param model
     * @return
     */
    @RequestMapping(value = "/details", method = RequestMethod.GET)
    public String details(Principal principal, Model model) {
        String login = principal.getName();
        Cart cart = cartService.getUserCart(login);
        if (cart.isDeliveryIncluded() == false) {
            return "redirect:/checkout/payment";
        }
        model.addAttribute("userContacts", contactsService.getUserContacts(login));
        return "checkout/details";
    }

    /**
     * Внесение изменений.
     *
     * @param principal
     * @param contacts
     * @param bindingResult
     * @param infoOption
     * @return
     */
    @RequestMapping(value = "/details", method = RequestMethod.PUT)
    public String putDetails(
            Principal principal,
            @Valid ContactsDTO contacts,
            BindingResult bindingResult,
            @RequestParam(value = "infoOption", required = true) String infoOption
    ) {
        if (infoOption.equals("useNew")) {
            if (bindingResult.hasErrors()) {
                return "checkout/details";
            }
            String login = principal.getName();
            contactsService.updateUserContacts(login, contacts);
        }
        return "redirect:/checkout/payment";
    }

    //------------------------------------------------------- Проверка и оплата
    
    /**
     * Страница оплаты.
     *
     * @param principal
     * @param model
     * @return
     */
        
    @RequestMapping(value = "/payment", method = RequestMethod.GET)
    public String payment(Principal principal, Model model) {
        String login = principal.getName();
        Cart cart = cartService.getUserCart(login);
        List<Product> products = new ArrayList<>();
        for (CartItem item : cart.getCartItems()) {
            products.add(item.getProduct());
        }
        model.addAttribute("products", products);
        model.addAttribute("userAccount", userAccountService.getUserAccount(login));
        model.addAttribute("contacts", contactsService.getUserContacts(login));
        model.addAttribute("deliveryCost", deliveryCost);
        model.addAttribute("creditCard", new CreditCardDTO());
        return "checkout/payment";
    }

    /**
     * Обработка формы оплаты.
     *
     * @param principal
     * @param creditCard
     * @param bindingResult
     * @param request
     * @return
     */
    @RequestMapping(value = "/payment", method = RequestMethod.POST)
    public String paymentPost(Principal principal, @Valid CreditCardDTO creditCard, BindingResult bindingResult, HttpServletRequest request) {
        String view = "checkout/payment";
        if (bindingResult.hasErrors()) {
            return view;
        }
        String login = principal.getName();
        try {
            Order order = orderService.createUserOrder(creditCard, login, deliveryCost);
            request.getSession().setAttribute("createdOrder", order.createDTO());
            return "redirect:/checkout/confirmation";
        } catch (EmptyCartException ex) {
            bindingResult.addError(ex.getFieldError());
            return view;
        }
    }

    //---------------------------------- Страница подтверждения и благодарности
    
    /**
     * Подтверждение и благодарность.
     *
     * @param principal
     * @param model
     * @param createdOrder
     * @return
     */
    @RequestMapping(value = "/confirmation", method = RequestMethod.GET)
    public String purchase(Principal principal, Model model, @ModelAttribute(value = "createdOrder") Order createdOrder) {
        String login = principal.getName();
        model.addAttribute("userAccount", userAccountService.getUserAccount(login));
        model.addAttribute("userDetails", contactsService.getUserContacts(login));
        return "checkout/confirmation";
    }
}
