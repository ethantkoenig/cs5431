<#import "master.ftl" as layout />

<@layout.master>
<!-- Page Content -->
<div class="container">

    <form action="/transact" method="post" id="transactform">
        <div class="form-group">
            <label>Recipient username</label>
            <input type="text" class="form-control" name="recipient">
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

</div>
<!-- /.container -->
</@layout.master>

