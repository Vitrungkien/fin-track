
$(document).ready(function () {
    setupDateSelects();
    loadBudgets();

    $('#budgetMonth, #budgetYear').change(loadBudgets);
});

function setupDateSelects() {
    const today = new Date();
    const currentYear = today.getFullYear();
    const currentMonth = today.getMonth() + 1;

    const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"];

    const monthSelect = $('#budgetMonth');
    monthNames.forEach((name, index) => {
        monthSelect.append(`<option value="${index + 1}" ${index + 1 === currentMonth ? 'selected' : ''}>${name}</option>`);
    });

    $('#budgetYear').append(`<option value="${currentYear}" selected>${currentYear}</option>`);
    $('#budgetYear').append(`<option value="${currentYear + 1}">${currentYear + 1}</option>`);
}

function loadBudgets() {
    const month = $('#budgetMonth').val();
    const year = $('#budgetYear').val();

    $.get(`/api/budgets/status?month=${month}&year=${year}`, function (data) {
        const container = $('#budgetList');
        container.empty();

        if (data.length === 0) {
            container.html('<div class="col-12 text-center text-muted p-5">No budgets set for this month</div>');
            return;
        }

        data.forEach(b => {
            const percent = Math.min(b.percentage, 100);
            let barColor = 'bg-success';
            if (b.percentage > 80) barColor = 'bg-warning';
            if (b.percentage > 100) barColor = 'bg-danger';

            container.append(`
                <div class="col-md-6 mb-4">
                    <div class="card h-100">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <h5 class="card-title mb-0">
                                    <span class="badge me-2" style="background-color: ${b.categoryColor}; color: white;">${b.categoryName}</span>
                                </h5>
                                <div class="dropdown">
                                    <button class="btn btn-sm btn-link text-muted" type="button" data-bs-toggle="dropdown">
                                        <i class="fas fa-ellipsis-v"></i>
                                    </button>
                                    <ul class="dropdown-menu">
                                        <li><a class="dropdown-item" href="#" onclick="editBudget(${b.budgetId}, ${b.budgetAmount})">Edit</a></li>
                                        <li><a class="dropdown-item text-danger" href="#" onclick="deleteBudget(${b.budgetId})">Delete</a></li>
                                    </ul>
                                </div>
                            </div>
                            
                            <div class="d-flex justify-content-between small mb-1">
                                <span>Spent: ${App.formatCurrency(b.spentAmount)}</span>
                                <span class="${b.isExceeded ? 'text-danger fw-bold' : ''}">Limit: ${App.formatCurrency(b.budgetAmount)}</span>
                            </div>
                            
                            <div class="progress" style="height: 20px;">
                                <div class="progress-bar ${barColor}" role="progressbar" 
                                     style="width: ${percent}%" aria-valuenow="${percent}" aria-valuemin="0" aria-valuemax="100">
                                     ${Math.round(b.percentage)}%
                                </div>
                            </div>
                            
                            <div class="mt-2 text-end small">
                                ${b.isExceeded
                    ? `<span class="text-danger"><i class="fas fa-exclamation-circle"></i> Over budget by ${App.formatCurrency(Math.abs(b.remainingAmount))}</span>`
                    : `<span class="text-muted">Remaining: ${App.formatCurrency(b.remainingAmount)}</span>`
                }
                            </div>
                        </div>
                    </div>
                </div>
            `);
        });
    });
}

function openBudgetModal() {
    $('#budgetId').val('');
    $('#amount').val('');
    $('#modalTitle').text('Set Budget');

    // Load Expense Categories
    $.get(`/api/categories?type=EXPENSE`, function (data) {
        const select = $('#categoryId');
        select.empty();
        data.forEach(cat => {
            select.append(`<option value="${cat.id}">${cat.name}</option>`);
        });
        $('#budgetModal').modal('show');
    });
}

function editBudget(id, amount) {
    $('#budgetId').val(id);
    $('#amount').val(amount);
    $('#categoryId').parent().hide(); // Hide category selection on edit for simplicity (or make it read-only)
    $('#modalTitle').text('Edit Budget');
    $('#budgetModal').modal('show');
}

function saveBudget() {
    const id = $('#budgetId').val();
    const data = {
        categoryId: $('#categoryId').val(),
        amount: $('#amount').val(),
        month: $('#budgetMonth').val(),
        year: $('#budgetYear').val()
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/budgets/${id}` : '/api/budgets';

    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function () {
            $('#budgetModal').modal('hide');
            $('#categoryId').parent().show(); // Reset visibility
            App.showToast('Budget saved');
            loadBudgets();
        },
        error: function (xhr) {
            alert('Error: ' + (xhr.responseJSON ? xhr.responseJSON.message : xhr.statusText));
        }
    });
}

function deleteBudget(id) {
    if (confirm('Delete this budget?')) {
        $.ajax({
            url: `/api/budgets/${id}`,
            type: 'DELETE',
            success: function () {
                App.showToast('Budget deleted');
                loadBudgets();
            }
        });
    }
}
