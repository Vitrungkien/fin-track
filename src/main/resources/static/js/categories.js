
let currentType = 'EXPENSE';

$(document).ready(function () {
    loadCategories('EXPENSE');
});

function loadCategories(type) {
    currentType = type;
    const container = type === 'EXPENSE' ? '#expenseCategories' : '#incomeCategories';

    $.get(`/api/categories?type=${type}`, function (data) {
        let html = '';
        if (data.length === 0) {
            html = '<div class="col-12 text-center text-muted p-5">No categories found</div>';
        } else {
            data.forEach(cat => {
                html += `
                    <div class="col-md-3 mb-3">
                        <div class="card h-100 category-card">
                            <div class="card-body d-flex justify-content-between align-items-center">
                                <div class="d-flex align-items-center">
                                    <div class="rounded-circle me-3" style="width: 20px; height: 20px; background-color: ${cat.color}"></div>
                                    <h6 class="mb-0">${cat.name}</h6>
                                </div>
                                <div>
                                    <button class="btn btn-sm btn-outline-primary me-1" onclick="editCategory(${cat.id}, '${cat.name}', '${cat.color}', '${cat.type}')">
                                        <i class="fas fa-edit"></i>
                                    </button>
                                    <button class="btn btn-sm btn-outline-danger" onclick="deleteCategory(${cat.id})">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
            });
        }
        $(container).html(html);
    });
}

function clearForm() {
    $('#categoryId').val('');
    $('#categoryName').val('');
    $('#categoryColor').val('#4A90E2');
    $('#categoryType').val(currentType);
    $('#modalTitle').text('Add Category');
}

function editCategory(id, name, color, type) {
    $('#categoryId').val(id);
    $('#categoryName').val(name);
    $('#categoryColor').val(color);
    $('#categoryType').val(type);
    $('#modalTitle').text('Edit Category');
    $('#categoryModal').modal('show');
}

function saveCategory() {
    const id = $('#categoryId').val();
    const data = {
        name: $('#categoryName').val(),
        color: $('#categoryColor').val(),
        type: $('#categoryType').val()
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/categories/${id}` : '/api/categories';

    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function () {
            $('#categoryModal').modal('hide');
            App.showToast('Category saved successfully');
            loadCategories(currentType);
            // Also reload other tab if type changed? For simplicity, assume modal stays on current type.
            // If user changed type in modal, we should reload that type.
            if (data.type !== currentType) {
                $(`#${data.type.toLowerCase()}-tab`).tab('show');
            }
        },
        error: function (xhr) {
            alert('Error saving category: ' + (xhr.responseJSON ? xhr.responseJSON.message : xhr.statusText));
        }
    });
}

function deleteCategory(id) {
    if (confirm('Are you sure you want to delete this category?')) {
        $.ajax({
            url: `/api/categories/${id}`,
            type: 'DELETE',
            success: function () {
                App.showToast('Category deleted successfully');
                loadCategories(currentType);
            },
            error: function () {
                alert('Failed to delete category. It may have related transactions.');
            }
        });
    }
}
