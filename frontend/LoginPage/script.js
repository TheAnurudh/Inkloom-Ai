const loginForm = document.getElementById("loginForm");
const emailInput = document.getElementById("email");
const passwordInput = document.getElementById("password");
const togglePassword = document.getElementById("togglePassword");
const rememberMe = document.getElementById("remember");
const emailError = document.getElementById("emailError");
const formMessage = document.getElementById("formMessage");
const passwordError = document.getElementById("passwordError")

//show/hide password
togglePassword.addEventListener("click" , function() {
    if(passwordInput.type == "password") {
        passwordInput.type = "text";
        togglePassword.setAttribute("aria-label" , "Hide password");
    }
    else {
        passwordInput.type = "password";
        togglePassword.setAttribute("aria-label" , "Show password")
    }
});

// error message during typing
emailInput.addEventListener("input" , function() {
    emailError.textContent = "";
    formMessage.textContent = "";
});

passwordInput.addEventListener("input" , function() {
    passwordError.textContent = "";
    formMessage.textContent = "";
});

//remember me
const savedEmail = localStorage.getItem("rememberEmail");

if(savedEmail) {
    emailInput.value = savedEmail;
    rememberMe.checked = true;
}

//Login form validation
loginForm.addEventListener("submit" , function (event){
    event.preventDefault();

    emailError.textContent = "";
    passwordError.textContent = "";
    formMessage.textContent = "";

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    let isValid = true;

    //email validation
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if(email == "") {
        emailError.textContent = "Please enter your email address.";
        isValid = false;
    }
    else if (!emailPattern.test(email)) {
        emailError.textContent = "Please enter a valid email address.";
        isValid = false;
    }

    //pass validation
    if (password.trim() == "") {
        passwordError.textContent = "Please enter your password."
        isValid = false;
    } 
    else if (password.length < 8) {
        passwordError.textContent = "Password must conatin at least 8 characters.";
        isValid = false;
    }

    if(!isValid) {
        return;
    }

    //Save email only , never save password
    if (rememberMe.checked) {
        localStorage.setItem("rememberEmail", email);
    }
    else {
        localStorage.removeItem("rememberEmail");
    }

    formMessage.style.color = "#218838";
    formMessage.textContent = "Validation Successful! Connect a backend to sign in."
});

//forgot password
document.getElementById("forgotPassword").addEventListener("click" , function (event) {
    event.preventDefault();

    formMessage.style.color = "#6048d5";
    formMessage.textContent = "Password reset will be available once authentication is connected.";

});

//request access
document.getElementById("requestAccessLink").addEventListener("click" , function (event) {
    event.preventDefault();

    formMessage.style.color = "#6048d5";
    formMessage.textContent = "Please contact your organisation administartor to request access.";

});

// google and microsoft button
document.querySelectorAll(".social-button").forEach(function (button) {
    button.addEventListener("click" , function () {
     const provider = button.dataset.provider;

     formMessage.style.color = "#6048d5";
formMessage.textContent = provider + " sign-in will work after authentication is connected.";
    });
});
