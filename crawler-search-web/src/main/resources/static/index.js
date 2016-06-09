$(function(){
    //提交表单
    $('form.searchForm').submit(function (event) {
        $('#waitModal').modal('show');
        event.preventDefault();
        $.get('/search?keyword=' + $('input.keyword').val(), function (data) {
            $('#waitModal').modal('hide');
            if (data.content) {
                var template = $('#template').html();
                Mustache.parse(template);
                var rendered = Mustache.render(template, data);
                $('div.list-group').html(rendered);
            }
        }).fail(function(e) {
            $('#waitModal').modal('hide');
            alert(e.responseJSON.message);
        });
    });

    //增量更新全部邮件
    $('button.update-all').click(function (e) {
        e.preventDefault();
        $('#progressModal .progress-bar').width('0%');
        $('#progressModal').modal('show');
        for(var i = 0; i< 100; i++){
            var n = 0;
            setTimeout(function () {
                $('#progressModal .progress-bar').width(++n + '%');
                if(n >= 100){
                    $('#progressModal').modal('hide');
                }
            }, i * 100);
        }
    });

    //更新当前邮件
    $(document).on('click', '.updateBtn',function (e) {
        $('#waitModal').modal('show');
        e.preventDefault();
        var $target = $(this);
        var id = $target.data('id'),
            url = $target.data('url');
        $.post('/update', {'id': id, 'url':url} , function (data) {
            $('#waitModal').modal('hide');
            $target.siblings('.result').text(data.result);
        });
    });
});
