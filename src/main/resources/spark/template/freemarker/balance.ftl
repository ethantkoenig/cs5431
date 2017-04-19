<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <div class="row">
        <div class="col-lg-12 text-center">
            <h1>Balance for ${loggedInUsername}</h1>
        </div>
    </div>

    <div class="row">
        <!-- TODO eventually put in a table and make it look nice -->
        <#list balances?keys as key>
          ${key}: ${balances[key]} <a class="delete-key" data-publickey="${key}">remove key</a> <br>
        </#list>
        Total: ${total}
    </div>
</div>
<!-- /.container -->
</@layout.master>
