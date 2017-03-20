<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <form action="/transact" method="post">
        <div class="form-group">
            <label>Input Transaction</label>
            <input type="text" class="form-control" name="transaction">
        </div>
        <div class="form-group">
            <label>Input Transaction Index</label>
            <input type="number" class="form-control" name="index">
        </div>
        <div class="form-group">
            <label>Your public key</label>
            <input type="text" class="form-control" name="senderpublickey">
        </div>
        <div class="form-group">
            <label>Recipient public key</label>
            <input type="text" class="form-control" name="recipientpublickey">
        </div>
        <div class="form-group">
            <label>Amount</label>
            <input type="number" class="form-control" name="amount">
        </div>
        <input class="btn btn-primary" type="submit" value="Make transaction">
    </form>

</div>
<!-- /.container -->
</@layout.master>

