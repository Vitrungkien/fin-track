
let currentPage = 0;
let pageSize = 10;

$(document).ready(function () {
    pageSize = $('#pageSize').val();
    setupFilters();
    loadFilterCategories();
    loadTransactions(0);

    // Auto-reload on filter change
    $('#filterMonth, #filterYear, #filterCategory').on('change', function () {
        loadTransactions(0);
    });

    // Handle Enter key on search input
    $('#filterKeyword').on('keypress', function (e) {
        if (e.which === 13) {
            loadTransactions(0);
        }
    });
});

function setupFilters() {
    const today = new Date();
    const currentYear = today.getFullYear();
    const currentMonth = today.getMonth() + 1;

    const monthSelect = $('#filterMonth');
    const yearSelect = $('#filterYear');

    // Years
    for (let i = currentYear; i >= currentYear - 5; i--) {
        yearSelect.append(`<option value="${i}" ${i === currentYear ? 'selected' : ''}>${i}</option>`);
    }

    // Months
    const monthNames = ["January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"];
    monthNames.forEach((name, index) => {
        monthSelect.append(`<option value="${index + 1}" ${index + 1 === currentMonth ? 'selected' : ''}>${name}</option>`);
    });
}

function loadTransactions(page = 0) {
    currentPage = page;
    pageSize = $('#pageSize').val();

    const month = $('#filterMonth').val();
    const year = $('#filterYear').val();
    const type = $('#filterType').val();
    const categoryId = $('#filterCategory').val();
    const keyword = $('#filterKeyword').val();

    let query = `?page=${page}&size=${pageSize}`;
    if (month) query += `&month=${month}`;
    if (year) query += `&year=${year}`;
    if (type) query += `&type=${type}`;
    if (categoryId) query += `&categoryId=${categoryId}`;
    if (keyword) query += `&keyword=${encodeURIComponent(keyword)}`;

    $.get(`/api/transactions${query}`, function (response) {
        const tbody = $('#transactionTableBody');
        tbody.empty();

        if (response.content.length === 0) {
            tbody.html('<tr><td colspan="5" class="text-center text-muted">No transactions found</td></tr>');
            updatePagination(response);
            return;
        }

        response.content.forEach(t => {
            const amountClass = t.type === 'INCOME' ? 'text-success' : 'text-danger';
            const sign = t.type === 'INCOME' ? '+' : '-';
            const categoryColor = t.categoryColor || '#6c757d';

            tbody.append(`
                <tr>
                    <td>${App.formatDateTime(t.transactionDate)}</td>
                    <td>
                        <span class="badge" style="background-color: ${categoryColor}">${t.categoryName}</span>
                    </td>
                    <td>${t.note || '-'}</td>
                    <td class="text-end ${amountClass}">
                        ${sign}${App.formatCurrency(t.amount)}
                    </td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-link" onclick="editTransaction(${t.id})"><i class="fas fa-edit"></i></button>
                        <button class="btn btn-sm btn-link text-danger" onclick="deleteTransaction(${t.id})"><i class="fas fa-trash"></i></button>
                    </td>
                </tr>
            `);
        });

        updatePagination(response);
    });
}

function changePageSize() {
    loadTransactions(0);
}

function updatePagination(pageData) {
    const totalPages = pageData.totalPages;
    const number = pageData.number;
    const size = pageData.size;
    const totalElements = pageData.totalElements;

    // Update info text
    const start = totalElements === 0 ? 0 : number * size + 1;
    const end = Math.min((number + 1) * size, totalElements);
    $('#pageInfo').text(`Showing ${start}-${end} of ${totalElements}`);

    const pagination = $('#paginationControls');
    pagination.empty();

    if (totalPages <= 1) return;

    // Previous
    const prevDisabled = number === 0 ? 'disabled' : '';
    pagination.append(`
        <li class="page-item ${prevDisabled}">
            <a class="page-link" href="#" onclick="loadTransactions(${number - 1}); return false;">Previous</a>
        </li>
    `);

    // Page Numbers - Simple sliding window
    const maxPagesToShow = 5;
    let startPage = Math.max(0, number - 2);
    let endPage = Math.min(totalPages - 1, startPage + maxPagesToShow - 1);

    if (endPage - startPage < maxPagesToShow - 1) {
        startPage = Math.max(0, endPage - maxPagesToShow + 1);
    }

    if (startPage > 0) {
        pagination.append(`
            <li class="page-item">
                <a class="page-link" href="#" onclick="loadTransactions(0); return false;">1</a>
            </li>
        `);
        if (startPage > 1) {
            pagination.append('<li class="page-item disabled"><span class="page-link">...</span></li>');
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        const active = i === number ? 'active' : '';
        pagination.append(`
            <li class="page-item ${active}">
                <a class="page-link" href="#" onclick="loadTransactions(${i}); return false;">${i + 1}</a>
            </li>
        `);
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            pagination.append('<li class="page-item disabled"><span class="page-link">...</span></li>');
        }
        pagination.append(`
            <li class="page-item">
                <a class="page-link" href="#" onclick="loadTransactions(${totalPages - 1}); return false;">${totalPages}</a>
            </li>
        `);
    }

    // Next
    const nextDisabled = number === totalPages - 1 ? 'disabled' : '';
    pagination.append(`
        <li class="page-item ${nextDisabled}">
            <a class="page-link" href="#" onclick="loadTransactions(${number + 1}); return false;">Next</a>
        </li>
    `);
}

function loadCategoriesForSelect(selectedCategoryId) {
    const type = $('#type').val();
    const categorySelect = $('#categoryId');

    $.get(`/api/categories?type=${type}`, function (data) {
        categorySelect.empty();
        data.forEach(cat => {
            const selected = (selectedCategoryId && cat.id == selectedCategoryId) ? 'selected' : '';
            categorySelect.append(`<option value="${cat.id}" ${selected}>${cat.name}</option>`);
        });
    });
}

function loadFilterCategories() {
    const type = $('#filterType').val();
    const categorySelect = $('#filterCategory');

    let url = '/api/categories';
    if (type) {
        url += `?type=${type}`;
    }

    $.get(url, function (data) {
        const currentVal = categorySelect.val();
        categorySelect.empty();
        categorySelect.append('<option value="">All Categories</option>');
        data.forEach(cat => {
            categorySelect.append(`<option value="${cat.id}">${cat.name}</option>`);
        });
        // Try to restore previous selection if it still exists
        categorySelect.val(currentVal);

        // When type changes, we should also reload transactions because the category selection might have changed/reset
        loadTransactions(0);
    });
}

function openAddModal() {
    $('#transactionId').val('');
    $('#transactionForm')[0].reset();

    // Set current local datetime
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    $('#transactionDate').val(now.toISOString().slice(0, 16));

    $('#type').val('EXPENSE');
    loadCategoriesForSelect();
    $('#modalTitle').text('Add Transaction');
    $('#transactionModal').modal('show');
}

function editTransaction(id) {
    $.get(`/api/transactions/${id}`, function (transaction) {
        $('#transactionId').val(transaction.id);
        $('#type').val(transaction.type);
        $('#amount').val(transaction.amount);

        // Ensure format matches datetime-local input
        let dateVal = transaction.transactionDate;
        if (dateVal && dateVal.length > 16) dateVal = dateVal.substring(0, 16);
        $('#transactionDate').val(dateVal);

        $('#note').val(transaction.note);

        loadCategoriesForSelect(transaction.categoryId);

        $('#modalTitle').text('Edit Transaction');
        $('#transactionModal').modal('show');
    }).fail(function () {
        // App might not be defined or available if common.js isn't loaded or fails, 
        // but assuming App.showToast is available as per original code.
        if (typeof App !== 'undefined' && App.showToast) {
            App.showToast('Error loading transaction details', 'danger');
        } else {
            alert('Error loading transaction details');
        }
    });
}

function saveTransaction() {
    const id = $('#transactionId').val();
    const data = {
        amount: $('#amount').val(),
        type: $('#type').val(),
        categoryId: $('#categoryId').val(),
        transactionDate: $('#transactionDate').val(),
        note: $('#note').val()
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/transactions/${id}` : '/api/transactions';

    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function () {
            $('#transactionModal').modal('hide');
            // Assuming App is defined in global scope from common.js
            if (typeof App !== 'undefined' && App.showToast) App.showToast('Transaction saved');

            // Reload current page to show updates
            loadTransactions(currentPage);
        },
        error: function (xhr) {
            alert('Error saving transaction: ' + (xhr.responseJSON ? xhr.responseJSON.message : xhr.statusText));
        }
    });
}

function deleteTransaction(id) {
    if (confirm('Delete this transaction?')) {
        $.ajax({
            url: `/api/transactions/${id}`,
            type: 'DELETE',
            success: function () {
                if (typeof App !== 'undefined' && App.showToast) App.showToast('Transaction deleted');
                loadTransactions(currentPage);
            }
        });
    }
}

function downloadTemplate() {
    window.location.href = '/api/transactions/template';
}

function openImportModal() {
    // Reset modal state
    $('#excelFile').val('');
    $('#importUploadSection').show();
    $('#importProgressSection').hide();
    $('#importResultSection').hide();
    $('#importButton').prop('disabled', false);

    $('#importModal').modal('show');
}

function importExcel() {
    const fileInput = $('#excelFile')[0];

    if (!fileInput.files || fileInput.files.length === 0) {
        alert('Please select an Excel file to import');
        return;
    }

    const file = fileInput.files[0];

    // Validate file extension
    const fileName = file.name.toLowerCase();
    if (!fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
        alert('Please select a valid Excel file (.xlsx or .xls)');
        return;
    }

    // Show progress
    $('#importUploadSection').hide();
    $('#importProgressSection').show();
    $('#importResultSection').hide();
    $('#importButton').prop('disabled', true);

    // Create FormData
    const formData = new FormData();
    formData.append('file', file);

    // Upload file
    $.ajax({
        url: '/api/transactions/import',
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function (result) {
            displayImportResult(result);

            // Reload transactions if any were imported successfully
            if (result.successCount > 0) {
                loadTransactions(currentPage);
            }
        },
        error: function (xhr) {
            $('#importProgressSection').hide();
            $('#importUploadSection').show();
            $('#importButton').prop('disabled', false);

            let errorMessage = 'Error importing file';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                errorMessage = xhr.responseJSON.message;
            } else if (xhr.statusText) {
                errorMessage = xhr.statusText;
            }

            alert('Import failed: ' + errorMessage);
        }
    });
}

function displayImportResult(result) {
    $('#importProgressSection').hide();
    $('#importResultSection').show();

    const alertDiv = $('#importResultAlert');
    const summaryDiv = $('#importResultSummary');
    const errorListDiv = $('#importErrorList');
    const errorsUl = $('#importErrors');

    // Set alert class based on result
    alertDiv.removeClass('alert-success alert-warning alert-danger');

    if (result.errorCount === 0) {
        alertDiv.addClass('alert-success');
    } else if (result.successCount > 0) {
        alertDiv.addClass('alert-warning');
    } else {
        alertDiv.addClass('alert-danger');
    }

    // Build summary message
    let summaryText = `Total rows processed: ${result.totalRows}<br>`;
    summaryText += `Successfully imported: ${result.successCount}<br>`;
    summaryText += `Failed: ${result.errorCount}`;
    summaryDiv.html(summaryText);

    // Show errors if any
    if (result.errors && result.errors.length > 0) {
        errorListDiv.show();
        errorsUl.empty();

        result.errors.forEach(error => {
            errorsUl.append(`<li>${error}</li>`);
        });
    } else {
        errorListDiv.hide();
    }

    // Show success toast if applicable
    if (result.successCount > 0 && typeof App !== 'undefined' && App.showToast) {
        App.showToast(`Successfully imported ${result.successCount} transaction(s)`, 'success');
    }
}
