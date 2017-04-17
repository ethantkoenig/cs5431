<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <form action="/transact" method="post" id="transactform">
        <div class="form-group">
            <label>Recipient username</label>
            <select class="form-control" name="recipient" id="recipient-selector">
            </select>
        </div>
        <div class="form-group">
            <label>Amount</label>
            <input type="number" class="form-control" name="amount">
        </div>
        <div class="form-group">
            <label>Password</label>
            <input type="password" class="form-control" id="transaction-password">
        </div>
        <input class="btn btn-primary" type="submit" value="Make transaction">
    </form>

    <#if friends??>
        <#list friends as friend>
            <script>
                $('#recipient-selector').append($('<option>', {value: "${friend}", text: "${friend}"}));
            </script>
        </#list>
    <#else>
        <script>
            $('#recipient-selector').append('<option value="" disabled>No one has authorized you to send them money.</option>');
        </script>
    </#if>

</div>
<!-- /.container -->
</@layout.master>

