
$(document).ready(function () {
    setupDateSelects();
    loadReport();

    $('#reportMonth, #reportYear').change(loadReport);
});

function setupDateSelects() {
    const today = new Date();
    const currentYear = today.getFullYear();
    const currentMonth = today.getMonth() + 1;

    const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"];

    const monthSelect = $('#reportMonth');
    monthNames.forEach((name, index) => {
        monthSelect.append(`<option value="${index + 1}" ${index + 1 === currentMonth ? 'selected' : ''}>${name}</option>`);
    });

    $('#reportYear').append(`<option value="${currentYear}" selected>${currentYear}</option>`);
    $('#reportYear').append(`<option value="${currentYear - 1}">${currentYear - 1}</option>`);
}

function loadReport() {
    const month = $('#reportMonth').val();
    const year = $('#reportYear').val();

    $.get(`/api/reports/monthly?month=${month}&year=${year}`, function (data) {
        $('#reportIncome').text(App.formatCurrency(data.totalIncome));
        $('#reportExpense').text(App.formatCurrency(data.totalExpense));
        $('#reportBalance').text(App.formatCurrency(data.balance));

        const tbody = $('#topCategoriesTable tbody');
        tbody.empty();

        if (data.topExpenseCategories.length === 0) {
            tbody.html('<tr><td colspan="3" class="text-center text-muted">No expense data</td></tr>');
        } else {
            data.topExpenseCategories.forEach(cat => {
                tbody.append(`
                    <tr>
                        <td>${cat.categoryName}</td>
                        <td class="text-end text-danger">${App.formatCurrency(cat.amount)}</td>
                        <td class="text-end">${cat.percentage.toFixed(1)}%</td>
                    </tr>
                `);
            });
        }
    });
}

function exportData(format) {
    const month = $('#reportMonth').val();
    const year = $('#reportYear').val();

    // Trigger download
    window.location.href = `/api/reports/export/${format}?month=${month}&year=${year}`;
}
