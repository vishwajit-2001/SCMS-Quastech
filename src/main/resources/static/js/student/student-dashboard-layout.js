document.addEventListener("DOMContentLoaded", function () {
    if ("scrollRestoration" in history) {
        history.scrollRestoration = "manual";
    }

    window.scrollTo(0, 0);

    const shell = document.querySelector(".student-shell");
    const toggle = document.getElementById("sidebarToggle");
    const storageKey = "student.sidebar.collapsed";

    const activeDashboardLink = document.querySelector('.portal-sidebar-nav .portal-nav-item.active[href*="dashboard"]');
    if (activeDashboardLink) {
        activeDashboardLink.addEventListener("click", function (event) {
            event.preventDefault();
            window.scrollTo(0, 0);
        });
    }

    if (shell && toggle) {
        if (window.sessionStorage.getItem(storageKey) === "true") {
            shell.classList.add("sidebar-collapsed");
        } else {
            shell.classList.remove("sidebar-collapsed");
        }

        toggle.addEventListener("click", function () {
            shell.classList.toggle("sidebar-collapsed");
            window.sessionStorage.setItem(storageKey, String(shell.classList.contains("sidebar-collapsed")));
        });
    }

    function parseTimeLabel(value) {
        if (!value) {
            return null;
        }

        const clean = value.trim().toUpperCase();
        const match = clean.match(/^(\d{1,2}):(\d{2})(?:\s*([AP]M))?$/);
        if (!match) {
            return null;
        }

        let hours = Number(match[1]);
        const minutes = Number(match[2]);
        const meridiem = match[3];

        if (Number.isNaN(hours) || Number.isNaN(minutes)) {
            return null;
        }

        if (meridiem === "PM" && hours < 12) {
            hours += 12;
        }
        if (meridiem === "AM" && hours === 12) {
            hours = 0;
        }

        return (hours * 60) + minutes;
    }

    function updateTimetableStates() {
        const now = new Date();
        const nowMinutes = (now.getHours() * 60) + now.getMinutes();
        const currentDay = now.toLocaleDateString("en-US", { weekday: "long" });

        function applyTimeState(element, start, end, stateLabel, progressBar) {
            element.classList.remove("is-current", "is-upcoming", "is-completed");

            if (start == null || end == null || end <= start) {
                element.classList.add("is-upcoming");
                if (stateLabel) {
                    stateLabel.textContent = "Scheduled";
                }
                if (progressBar) {
                    progressBar.style.width = "30%";
                }
                return;
            }

            if (nowMinutes >= end) {
                element.classList.add("is-completed");
                if (stateLabel) {
                    stateLabel.textContent = "Completed";
                }
                if (progressBar) {
                    progressBar.style.width = "100%";
                }
                return;
            }

            if (nowMinutes >= start && nowMinutes < end) {
                element.classList.add("is-current");
                if (stateLabel) {
                    stateLabel.textContent = "Live Now";
                }
                if (progressBar) {
                    const progress = ((nowMinutes - start) / (end - start)) * 100;
                    progressBar.style.width = Math.max(18, Math.min(100, progress)) + "%";
                }
                return;
            }

            element.classList.add("is-upcoming");
            if (stateLabel) {
                stateLabel.textContent = "Upcoming";
            }
            if (progressBar) {
                progressBar.style.width = "32%";
            }
        }

        document.querySelectorAll(".portal-list-item--slot").forEach(function (slot) {
            const start = parseTimeLabel(slot.dataset.start);
            const end = parseTimeLabel(slot.dataset.end);
            const stateLabel = slot.querySelector(".portal-slot-state");
            const progressBar = slot.querySelector(".portal-slot-progress-bar");
            applyTimeState(slot, start, end, stateLabel, progressBar);
        });

        document.querySelectorAll(".portal-schedule-cell[data-day]").forEach(function (cell) {
            if (cell.dataset.slotType === "break") {
                cell.classList.add("is-break");
                const stateLabel = cell.querySelector(".portal-schedule-status");
                if (stateLabel) {
                    stateLabel.textContent = "Lunch Break";
                }
                return;
            }

            const start = parseTimeLabel(cell.dataset.start);
            const end = parseTimeLabel(cell.dataset.end);
            const stateLabel = cell.querySelector(".portal-schedule-status");

            cell.classList.remove("is-current", "is-upcoming", "is-completed");

            if (cell.dataset.day !== currentDay) {
                if (stateLabel) {
                    stateLabel.textContent = "Scheduled";
                }
                return;
            }

            applyTimeState(cell, start, end, stateLabel, null);
        });
    }

    function updateLiveClocks() {
        const formatted = new Date().toLocaleTimeString([], {
            hour: "numeric",
            minute: "2-digit",
            hour12: true
        });

        document.querySelectorAll(".js-live-clock").forEach(function (clock) {
            clock.textContent = formatted;
        });
    }

    updateTimetableStates();
    updateLiveClocks();
    window.setInterval(function () {
        updateTimetableStates();
        updateLiveClocks();
    }, 60000);
});
