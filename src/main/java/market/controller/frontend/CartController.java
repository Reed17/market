package market.controller.frontend;

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import market.domain.Cart;
import market.domain.dto.CartDTO;
import market.domain.dto.CartItemDTO;
import market.exception.UnknownProductException;
import market.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Контроллер корзины.
 */
@Controller
@RequestMapping("/cart")
@SessionAttributes({"cart"})
public class CartController {

    @Value("${deliveryCost}")
    private int deliveryCost;
    
    @Autowired
    private CartService cartService;

    /**
     * Страница корзины.
     * 
     * @param principal
     * @param request
     * @param model
     * @return 
     */
    @RequestMapping(method = RequestMethod.GET)
    public String getCart(Principal principal, HttpServletRequest request, Model model) {
        if (principal != null) {
            Cart cart = cartService.getUserCart(principal.getName());
            request.getSession().setAttribute("cart", cart);
        }
        model.addAttribute("deliveryCost", deliveryCost);
        return "cart";
    }

    /**
     * Очистка корзины.
     * 
     * @param principal
     * @param cart
     * @return 
     */
    @RequestMapping(method = RequestMethod.DELETE)
    public String clearCart(Principal principal, @ModelAttribute(value = "cart") Cart cart) {
        if (principal != null) {
            cartService.clearUserCart(principal.getName());
        }
        cart.clear();
        return "redirect:/cart";
    }

    //--------------------------------------------- Добавление товара в корзину
    
    /**
     * Добавление через форму.
     * 
     * @param principal
     * @param cartItem
     * @param bindingResult
     * @param cart
     * @return 
     */
    @RequestMapping(method = RequestMethod.PUT)
    public String updateCartByForm(Principal principal, @Valid @ModelAttribute("cartItem") CartItemDTO cartItem, BindingResult bindingResult, @ModelAttribute(value = "cart") Cart cart) {
        String view = "cart";
        if (bindingResult.hasErrors()) {
            return view;
        }
        try {
            updateCart(cart, cartItem, principal);
        } catch (UnknownProductException ex) {
            bindingResult.addError(ex.getFieldError());
            return view;
        }
        return "redirect:/cart";
    }

    /**
     * Добавление через объект JSON.
     * 
     * @param principal
     * @param cartItem
     * @param bindingResult
     * @param cart
     * @return 
     * @throws UnknownProductException
     */
    @RequestMapping(method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CartDTO updateCartByAjax(Principal principal, @Valid @RequestBody CartItemDTO cartItem, BindingResult bindingResult, @ModelAttribute(value = "cart") Cart cart) throws UnknownProductException {
        if (bindingResult.hasErrors()) {
            return cart.createAnonymousDTO(deliveryCost);
        }
        updateCart(cart, cartItem, principal);
        return cart.createAnonymousDTO(deliveryCost);
    }

    private void updateCart(Cart cart, CartItemDTO cartItem, Principal principal) throws UnknownProductException {
        cartService.updateCartObject(cart, cartItem);
        if (principal != null) {
            String login = principal.getName();
            cartService.updateUserCart(login, cartItem);
        }
    }

    //---------------------------------------------- Установка способа доставки
    
    /**
     * Установка способа доставки через объект JSON.
     * 
     * @param principal
     * @param delivery
     * @param cart
     * @return 
     */
    @RequestMapping(value = "/delivery/{delivery}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CartDTO setDelivery(Principal principal, @PathVariable String delivery, @ModelAttribute(value = "cart") Cart cart) {
        Boolean included = Boolean.valueOf(delivery);
        if (principal != null) {
            String login = principal.getName();
            cartService.setUserCartDelivery(login, included);
        }
        cart.setDeliveryIncluded(included);
        return cart.createAnonymousDTO(deliveryCost);
    }
}
