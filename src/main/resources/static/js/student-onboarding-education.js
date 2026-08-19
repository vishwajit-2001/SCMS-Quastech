(function () {
    function textValue(field) {
        if (!field) return "";
        return (field.value || "").trim();
    }

    function numberValue(field) {
        const raw = textValue(field);
        if (!raw) return null;
        const parsed = Number(raw);
        return Number.isFinite(parsed) ? parsed : null;
    }

    function formatPercent(obtained, total) {
        if (obtained == null || total == null || total <= 0) return "";
        const value = Math.round(((obtained / total) * 100) * 100) / 100;
        return value % 1 === 0 ? String(Math.trunc(value)) : value.toFixed(2);
    }

    function createCell(text) {
        const td = document.createElement("td");
        td.textContent = text;
        return td;
    }

    document.addEventListener("DOMContentLoaded", function () {
        const panel = document.querySelector('[data-step-panel="education"]');
        if (!panel) return;

        const form = panel.closest("form");
        const historyBody = panel.querySelector("[data-education-history]");
        const addButton = panel.querySelector("[data-education-add]");
        const fields = {
            lastQualifyingExamName: panel.querySelector('[data-education-field="lastQualifyingExamName"]'),
            examName: panel.querySelector('[data-education-field="examName"]'),
            boardUniversity: panel.querySelector('[data-education-field="boardUniversity"]'),
            schoolCollege: panel.querySelector('[data-education-field="schoolCollege"]'),
            dateOfPassing: panel.querySelector('[data-education-field="dateOfPassing"]'),
            result: panel.querySelector('[data-education-field="result"]'),
            passingYear: panel.querySelector('[data-education-field="passingYear"]'),
            examSeatNo: panel.querySelector('[data-education-field="examSeatNo"]'),
            obtainedMarks: panel.querySelector('[data-education-field="obtainedMarks"]'),
            totalMarks: panel.querySelector('[data-education-field="totalMarks"]'),
            percentage: panel.querySelector('[data-education-field="percentage"]'),
            passingMonth: panel.querySelector('[data-education-field="passingMonth"]'),
        };

        const history = [];

        function renderHistory() {
            if (!historyBody) return;
            historyBody.innerHTML = "";
            if (!history.length) {
                const emptyRow = document.createElement("tr");
                emptyRow.setAttribute("data-empty-row", "true");
                const cell = document.createElement("td");
                cell.colSpan = 6;
                cell.textContent = "No qualification added yet.";
                cell.style.textAlign = "center";
                cell.style.color = "#8a97ab";
                cell.style.padding = "16px 8px";
                emptyRow.appendChild(cell);
                historyBody.appendChild(emptyRow);
                return;
            }

            history.forEach((item, index) => {
                const row = document.createElement("tr");
                row.appendChild(createCell(item.examLevel));
                row.appendChild(createCell(item.examName));
                row.appendChild(createCell(item.total));
                row.appendChild(createCell(item.obtained));
                row.appendChild(createCell(item.percentage ? `${item.percentage}%` : ""));

                const deleteCell = document.createElement("td");
                const deleteButton = document.createElement("button");
                deleteButton.type = "button";
                deleteButton.className = "btn";
                deleteButton.textContent = "Delete";
                deleteButton.style.minHeight = "24px";
                deleteButton.style.padding = "4px 8px";
                deleteButton.style.background = "#fff3f3";
                deleteButton.style.color = "#c23a3a";
                deleteButton.style.borderColor = "#efc4c4";
                deleteButton.addEventListener("click", function () {
                    history.splice(index, 1);
                    renderHistory();
                });
                deleteCell.appendChild(deleteButton);
                row.appendChild(deleteCell);
                historyBody.appendChild(row);
            });
        }

        function syncPercentage() {
            const obtained = numberValue(fields.obtainedMarks);
            const total = numberValue(fields.totalMarks);
            const percentage = formatPercent(obtained, total);
            if (fields.percentage) {
                fields.percentage.value = percentage;
            }
        }

        function validateCurrentEntry() {
            const required = [
                fields.lastQualifyingExamName,
                fields.examName,
                fields.boardUniversity,
                fields.schoolCollege,
                fields.dateOfPassing,
                fields.result,
                fields.passingYear,
                fields.examSeatNo,
                fields.obtainedMarks,
                fields.totalMarks,
                fields.percentage,
                fields.passingMonth
            ];
            for (const field of required) {
                if (!field || !textValue(field)) {
                    field?.focus();
                    return false;
                }
            }
            const year = textValue(fields.passingYear);
            if (!/^\d{4}$/.test(year)) {
                fields.passingYear.focus();
                return false;
            }
            const obtained = numberValue(fields.obtainedMarks);
            const total = numberValue(fields.totalMarks);
            if (obtained == null || total == null || total <= 0 || obtained > total) {
                fields.obtainedMarks.focus();
                return false;
            }
            syncPercentage();
            return true;
        }

        function addEntry() {
            if (!validateCurrentEntry()) return;
            const entry = {
                examLevel: textValue(fields.lastQualifyingExamName),
                examName: textValue(fields.examName),
                board: textValue(fields.boardUniversity),
                institute: textValue(fields.schoolCollege),
                dateOfPassing: textValue(fields.dateOfPassing),
                result: textValue(fields.result),
                year: textValue(fields.passingYear),
                seatNo: textValue(fields.examSeatNo),
                obtained: textValue(fields.obtainedMarks),
                total: textValue(fields.totalMarks),
                percentage: textValue(fields.percentage),
                month: textValue(fields.passingMonth)
            };
            history.push(entry);
            renderHistory();
        }

        function bind(field) {
            if (!field) return;
            field.addEventListener("input", syncPercentage);
            field.addEventListener("change", syncPercentage);
        }

        bind(fields.obtainedMarks);
        bind(fields.totalMarks);
        syncPercentage();

        if (addButton) {
            addButton.addEventListener("click", addEntry);
        }

        if (form) {
            form.addEventListener("submit", function () {
                syncPercentage();
            });
        }

        renderHistory();
    });
})();
