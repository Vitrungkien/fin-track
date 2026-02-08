function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
}

$(document).ready(function () {
    const today = new Date();
    const currentMonth = today.getMonth() + 1;
    const currentYear = today.getFullYear();

    // Populate Month/Year Selects
    const monthSelect = $('#monthSelect');
    const yearSelect = $('#yearSelect');

    const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"];

    monthNames.forEach((name, index) => {
        monthSelect.append(`<option value="${index + 1}" ${index + 1 === currentMonth ? 'selected' : ''}>${name}</option>`);
    });

    for (let i = currentYear; i >= currentYear - 5; i--) {
        yearSelect.append(`<option value="${i}" ${i === currentYear ? 'selected' : ''}>${i}</option>`);
    }

    // Chart instances
    let dailyChart = null;
    let categoryChart = null;

    function loadDashboardData() {
        const month = monthSelect.val();
        const year = yearSelect.val();

        // Load Summary
        $.get(`/api/dashboard/summary?month=${month}&year=${year}`, function (data) {
            $('#totalIncome').text(formatCurrency(data.totalIncome));
            $('#totalExpense').text(formatCurrency(data.totalExpense));
            $('#balance').text(formatCurrency(data.balance));
        });

        // Load Daily Chart
        $.get(`/api/dashboard/chart/daily?month=${month}&year=${year}`, function (data) {
            const ctx = document.getElementById('dailyCtx').getContext('2d');

            if (dailyChart) dailyChart.destroy();

            dailyChart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: data.labels,
                    datasets: [{
                        label: 'Expense',
                        data: data.data,
                        backgroundColor: '#4A90E2',
                        borderRadius: 4
                    }]
                },
                options: {
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        y: { beginAtZero: true }
                    }
                }
            });
        });

        // Load Category Chart
        $.get(`/api/dashboard/chart/category?month=${month}&year=${year}`, function (data) {
            const ctx = document.getElementById('categoryCtx').getContext('2d');

            if (categoryChart) categoryChart.destroy();

            categoryChart = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: data.labels,
                    datasets: [{
                        data: data.data,
                        backgroundColor: data.colors
                    }]
                },
                options: {
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom' }
                    }
                }
            });
        });

        // Load Category Summary List
        loadCategorySummary(month, year);
    }

    function loadCategorySummary(month, year) {
        $.get(`/api/dashboard/category-summary?month=${month}&year=${year}`, function (data) {
            const list = $('#categoryList');
            list.empty();

            if (data.length === 0) {
                list.html('<div class="list-group-item text-center text-muted">No expenses for this period.</div>');
                return;
            }

            data.forEach(item => {
                const iconClass = item.icon || 'fas fa-tag';
                const itemHtml = `
                    <div class="list-group-item list-group-item-action p-0 border-bottom">
                        <div class="d-flex w-100 justify-content-between align-items-center category-header p-3" 
                             style="cursor: pointer;" 
                             data-category-id="${item.categoryId}"
                             data-month="${month}"
                             data-year="${year}">
                            <div class="d-flex align-items-center">
                                <div class="rounded-circle me-3 d-flex justify-content-center align-items-center" 
                                     style="width: 40px; height: 40px; background-color: ${item.color}; color: white;">
                                    <i class="${iconClass}"></i>
                                </div>
                                <h6 class="mb-0 text-gray-800">${item.categoryName}</h6>
                            </div>
                            <div class="d-flex align-items-center">
                                <span class="badge bg-light text-dark me-3">${item.percentage.toFixed(1)}%</span>
                                <h6 class="mb-0 font-weight-bold me-3 text-danger">${formatCurrency(item.totalAmount)}</h6>
                                <i class="fas fa-chevron-down toggle-icon transition-transform"></i>
                            </div>
                        </div>
                        <div class="category-details collapse bg-light" id="details-${item.categoryId}">
                            <div class="p-3">
                                <div class="text-center py-2"><div class="spinner-border spinner-border-sm text-primary" role="status"></div></div>
                            </div>
                        </div>
                    </div>
                `;
                list.append(itemHtml);
            });
        });
    }

    // Toggle Category Details
    $(document).on('click', '.category-header', function () {
        const categoryId = $(this).data('category-id');
        const month = $(this).data('month');
        const year = $(this).data('year');

        const detailsDiv = $(`#details-${categoryId}`);
        const icon = $(this).find('.toggle-icon');

        if (detailsDiv.hasClass('show')) {
            detailsDiv.removeClass('show');
            icon.removeClass('fa-rotate-180');
        } else {
            detailsDiv.addClass('show');
            icon.addClass('fa-rotate-180');

            // Load transactions if spinner is present
            if (detailsDiv.find('.spinner-border').length > 0) {
                $.get(`/api/transactions?categoryId=${categoryId}&month=${month}&year=${year}&type=EXPENSE&size=100`, function (response) {
                    const transactions = response.content;
                    const container = detailsDiv.find('div.p-3');
                    container.empty();

                    if (transactions.length === 0) {
                        container.html('<p class="text-muted text-center small mb-0">No transactions found.</p>');
                        return;
                    }

                    const table = `
                        <div class="table-responsive">
                            <table class="table table-sm table-borderless mb-0 small bg-white rounded shadow-sm">
                                <thead class="bg-light">
                                    <tr class="text-muted border-bottom">
                                        <th class="ps-3">Date</th>
                                        <th>Note</th>
                                        <th class="text-end pe-3">Amount</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${transactions.map(t => `
                                        <tr class="border-bottom">
                                            <td class="ps-3">${new Date(t.transactionDate).toLocaleDateString()}</td>
                                            <td>${t.note || '-'}</td>
                                            <td class="text-end pe-3 font-weight-bold text-danger">${formatCurrency(t.amount)}</td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `;
                    container.html(table);
                });
            }
        }
    });

    // Add generic styles
    $('head').append('<style>.transition-transform { transition: transform 0.3s ease; } .fa-rotate-180 { transform: rotate(180deg); }</style>');

    // Initial Load
    loadDashboardData();

    // Reload on change
    $('#monthSelect, #yearSelect').change(loadDashboardData);
});
