const pasteBtn = document.getElementById("pasteBtn");
const uploadBtn = document.getElementById("uploadBtn");
const imageBtn = document.getElementById("imageBtn");
const linkBtn = document.getElementById("linkBtn");
const fileInput = document.getElementById("fileInput");
const imageInput = document.getElementById("imageInput");
const attachBtn = document.getElementById("attachBin");
const chatForm = document.getElementById("chatForm");
const messageInput = document.getElementById("messageInput");
const chatArea = document.querySelector(".chat-area");

// Text paste button
pasteBtn.addEventListener("click", async () => {
    try {
        const text = await navigator.clipboard.readText();

        if (text) {
            messageInput.value = text;
        } else {
            messageInput.placeholder = "Paste your text here...";
        }
    } catch {
        messageInput.placeholder =
            "Paste your text here using Ctrl + V";
    }

    messageInput.focus();
});

// Upload file
uploadBtn.addEventListener("click", () => {
    fileInput.click();
});

fileInput.addEventListener("change", () => {
    if (fileInput.files.length > 0) {
        messageInput.value =
            `Selected file: ${fileInput.files[0].name}`;
    }
});

// Add image
imageBtn.addEventListener("click", () => {
    imageInput.click();
});

imageInput.addEventListener("change", () => {
    if (imageInput.files.length > 0) {
        messageInput.value =
            `Selected image: ${imageInput.files[0].name}`;
    }
});

// Attach file button
attachBtn.addEventListener("click", () => {
    fileInput.click();
});

// Add link
linkBtn.addEventListener("click", () => {
    const link = prompt("Enter the URL:");

    if (link && link.trim()) {
        messageInput.value = link.trim();
        messageInput.focus();
    }
});

// Show messages in chat
function addMessage(text, type = "user") {
    const message = document.createElement("div");

    message.className =
        type === "user"
            ? "chat-message"
            : "chat-message assistant-message";

    message.textContent = text;
    chatArea.appendChild(message);

    message.scrollIntoView({
        behavior: "smooth",
        block: "nearest"
    });
}

// Send message
chatForm.addEventListener("submit", (event) => {
    event.preventDefault();

    const text = messageInput.value.trim();

    if (!text) return;

    addMessage(text, "user");

    messageInput.value = "";
    messageInput.focus();

    // Demo response only; a real AI needs a backend/API
    setTimeout(() => {
        addMessage(
            "Your content has been received. Connect an AI backend to review it against brand guidelines and suggest improvements.",
            "assistant"
        );
    }, 400);
});
