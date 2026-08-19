document.addEventListener("DOMContentLoaded", function () {
    let roots = Array.from(document.querySelectorAll(".scms-chatbot"));
    if (roots.length === 0) {
        const fallback = createFallbackChatbotRoot();
        if (fallback) {
            document.body.appendChild(fallback);
            roots = [fallback];
        }
    }
    roots.forEach(initChatbot);
});

function createFallbackChatbotRoot() {
    const role = document.body.classList.contains("teacher-layout-page") || window.location.pathname.startsWith("/teacher")
        ? "TEACHER"
        : "STUDENT";
    const displayNameEl = document.querySelector(".portal-user-name");
    const collegeEl = document.querySelector(".portal-hero-college-name, .portal-hero-college-name--side");
    const displayName = displayNameEl ? displayNameEl.textContent.trim() : "";
    const collegeName = collegeEl ? collegeEl.textContent.trim() : "";
    const root = document.createElement("div");
    root.className = "scms-chatbot";
    root.dataset.role = role;
    root.dataset.displayName = displayName;
    root.dataset.collegeName = collegeName;
    root.dataset.messageEndpoint = "/api/chatbot/message";
    root.dataset.contextEndpoint = "/api/chatbot/context";
    root.innerHTML = `
        <button class="scms-chatbot-launcher" type="button" aria-expanded="false" aria-label="Open CHATBOT">
            <span class="scms-chatbot-launcher-mark" aria-hidden="true">
                <span class="scms-chatbot-launcher-dot scms-chatbot-launcher-dot--blue"></span>
                <span class="scms-chatbot-launcher-dot scms-chatbot-launcher-dot--orange"></span>
            </span>
            <span class="scms-chatbot-launcher-copy"><strong>CHATBOT</strong></span>
        </button>
        <div class="scms-chatbot-shell" hidden>
            <button class="scms-chatbot-backdrop" type="button" aria-label="Close assistant"></button>
            <section class="scms-chatbot-panel" role="dialog" aria-modal="true" aria-label="Campus Assistant">
                <header class="scms-chatbot-header">
                    <div class="scms-chatbot-header-copy">
                        <span class="scms-chatbot-kicker">${role === "TEACHER" ? "Teacher console" : "Student console"}</span>
                        <h2 class="scms-chatbot-title">Campus Assistant</h2>
                        <p class="scms-chatbot-subtitle">${collegeName || "College"}</p>
                    </div>
                    <div class="scms-chatbot-header-actions">
                        <div class="scms-chatbot-status">
                            <span class="scms-chatbot-status-dot"></span>
                            <span>Live</span>
                        </div>
                        <button class="scms-chatbot-close" type="button" aria-label="Close assistant">
                            <i class="fa-solid fa-xmark"></i>
                        </button>
                    </div>
                </header>
                <div class="scms-chatbot-body">
                    <div class="scms-chatbot-intro">
                        <span class="scms-chatbot-intro-label">Quick help</span>
                        <p>Ask about timetable, fees, subjects, assignments, or profile details.</p>
                    </div>
                    <div class="scms-chatbot-messages" aria-live="polite" aria-relevant="additions"></div>
                    <div class="scms-chatbot-suggestions" aria-label="Quick prompts"></div>
                </div>
                <form class="scms-chatbot-form" autocomplete="off">
                    <label class="sr-only" for="scmsChatInput">Ask the assistant</label>
                    <textarea id="scmsChatInput" class="scms-chatbot-input" rows="1" placeholder="Ask about timetable, fees, students..." maxlength="1000"></textarea>
                    <button type="submit" class="scms-chatbot-send">
                        <i class="fa-solid fa-paper-plane"></i>
                        Send
                    </button>
                </form>
            </section>
        </div>
    `;
    return root;
}

function initChatbot(root) {
    const launcher = root.querySelector(".scms-chatbot-launcher");
    const shell = root.querySelector(".scms-chatbot-shell");
    const backdrop = root.querySelector(".scms-chatbot-backdrop");
    const closeButton = root.querySelector(".scms-chatbot-close");
    const panel = root.querySelector(".scms-chatbot-panel");
    const messages = root.querySelector(".scms-chatbot-messages");
    const suggestions = root.querySelector(".scms-chatbot-suggestions");
    const form = root.querySelector(".scms-chatbot-form");
    const input = root.querySelector(".scms-chatbot-input");
    const sendButton = root.querySelector(".scms-chatbot-send");
    const title = root.querySelector(".scms-chatbot-title");
    const subtitle = root.querySelector(".scms-chatbot-subtitle");

    let historyLoaded = false;

    const contextEndpoint = root.dataset.contextEndpoint;
    const messageEndpoint = root.dataset.messageEndpoint;

    root.classList.remove("is-open");
    shell.hidden = true;
    shell.style.display = "none";
    launcher.setAttribute("aria-expanded", "false");

    launcher.addEventListener("click", openChat);
    backdrop.addEventListener("click", closeChat);
    if (closeButton) {
        closeButton.addEventListener("click", closeChat);
    }

    form.addEventListener("submit", function (event) {
        event.preventDefault();
        sendMessage();
    });

    input.addEventListener("keydown", function (event) {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            sendMessage();
        }
    });

    window.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && root.classList.contains("is-open")) {
            closeChat();
        }
    });

    autoSizeInput(input);

    function openChat() {
        root.classList.add("is-open");
        shell.hidden = false;
        shell.style.display = "grid";
        launcher.setAttribute("aria-expanded", "true");
        if (!historyLoaded) {
            loadContext();
        }
        setTimeout(function () {
            input.focus();
        }, 50);
    }

    function closeChat() {
        root.classList.remove("is-open");
        shell.hidden = true;
        shell.style.display = "none";
        launcher.setAttribute("aria-expanded", "false");
    }

    async function loadContext() {
        try {
            setBusy(true);
            const response = await fetch(contextEndpoint, {
                headers: { "Accept": "application/json" }
            });

            if (!response.ok) {
                renderSystemMessage("Please sign in again to continue.");
                return;
            }

            const data = await response.json();
            applyConversation(data, true);
            historyLoaded = true;
        } catch (error) {
            renderSystemMessage("Chatbot is temporarily unavailable.");
        } finally {
            setBusy(false);
        }
    }

    async function sendMessage() {
        const text = input.value.trim();
        if (!text || sendButton.disabled) {
            return;
        }

        renderUserMessage(text);
        input.value = "";
        autoSizeInput(input);
        setBusy(true);

        try {
            const response = await fetch(messageEndpoint, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({ message: text })
            });

            if (!response.ok) {
                renderSystemMessage("Your session expired. Please log in again.");
                return;
            }

            const data = await response.json();
            applyConversation(data, false);
            historyLoaded = true;
        } catch (error) {
            renderSystemMessage("I could not reach the chatbot service.");
        } finally {
            setBusy(false);
            input.focus();
        }
    }

    function applyConversation(data, replaceExisting) {
        if (replaceExisting) {
            messages.innerHTML = "";
        }

        if (title && data.title) {
            title.textContent = data.title;
        }
        if (subtitle && data.subtitle) {
            subtitle.textContent = data.subtitle;
        }

        if (Array.isArray(data.messages) && data.messages.length) {
            messages.innerHTML = "";
            data.messages.forEach(function (message) {
                appendMessage(message.sender || "assistant", message.text || "", message.timestamp || "");
            });
        } else if (data.greeting && replaceExisting) {
            renderAssistantMessage(data.greeting, "");
        }

        renderSuggestions(Array.isArray(data.suggestions) ? data.suggestions : []);
        scrollToBottom();
    }

    function renderSuggestions(items) {
        suggestions.innerHTML = "";
        items.forEach(function (item) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "scms-chatbot-suggestion";
            button.textContent = item;
            button.addEventListener("click", function () {
                input.value = item;
                autoSizeInput(input);
                sendMessage();
            });
            suggestions.appendChild(button);
        });
    }

    function renderUserMessage(text) {
        appendMessage("user", text, "");
        scrollToBottom();
    }

    function renderAssistantMessage(text, timestamp) {
        appendMessage("assistant", text, timestamp);
        scrollToBottom();
    }

    function renderSystemMessage(text) {
        appendMessage("assistant", text, "");
        scrollToBottom();
    }

    function appendMessage(sender, text, timestamp) {
        const bubble = document.createElement("div");
        bubble.className = "scms-chatbot-message " + (sender === "user" ? "scms-chatbot-message--user" : "scms-chatbot-message--assistant");
        bubble.textContent = text;

        if (timestamp) {
            const meta = document.createElement("div");
            meta.className = "scms-chatbot-message-meta";
            meta.textContent = timestamp;
            bubble.appendChild(meta);
        }

        messages.appendChild(bubble);
    }

    function setBusy(isBusy) {
        sendButton.disabled = isBusy;
        input.disabled = isBusy;
    }

    function scrollToBottom() {
        messages.scrollTop = messages.scrollHeight;
    }

    function autoSizeInput(field) {
        field.style.height = "auto";
        field.style.height = Math.min(field.scrollHeight, 180) + "px";
    }

    if (panel) {
        panel.addEventListener("click", function (event) {
            event.stopPropagation();
        });
    }

    renderSuggestions([
        "My timetable",
        "Pending fees",
        "My subjects",
        "My profile"
    ]);
}
