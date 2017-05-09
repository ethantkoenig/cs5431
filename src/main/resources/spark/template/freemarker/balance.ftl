<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>Balance for ${loggedInUsername}<span class="label label-default pull-right">Total: ${total}</span></h1>
        </div>
        <h1></h1>
    </div>

    <div class="row padding-top">

        <#list balances?keys as key>
        <table class="table">
            <tr>
                <th>Key</th>
                <th>Balance</th>
                <th></th>
            </tr>
            <tr>
                <td>${key}</td>
                <td>${balances[key]}</td>
                <td><a class="delete-key button" data-publickey="${key}">remove key</a></td>
            </tr>
        </#list>
    </div>
</div>
<!-- /.container -->
</@layout.master>
