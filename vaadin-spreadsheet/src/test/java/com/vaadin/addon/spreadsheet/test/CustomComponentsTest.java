package com.vaadin.addon.spreadsheet.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.server.browserlaunchers.Sleeper;
import org.openqa.selenium.support.ui.Select;

import com.vaadin.addon.spreadsheet.elements.SheetCellElement;
import com.vaadin.addon.spreadsheet.elements.SpreadsheetElement;

public class CustomComponentsTest extends Test1 {

    final static String TEXT_PROXY = "text";
    final static Integer NUM_PROXY = 42;

    @Test
    @Ignore("Fails with all IE")
    public void testTextField() {
        loadServerFixture("CUSTOM_COMPONENTS");

        SheetCellElement b2 = $(SpreadsheetElement.class).first().getCellAt(
                "B2");
        typeInTextFieldEditor(b2, TEXT_PROXY);

        sheetController.selectCell("B3");
        sheetController.insertAndRet("=B2");

        Assert.assertEquals(TEXT_PROXY, sheetController.getCellContent("B2"));
        Assert.assertEquals(TEXT_PROXY, sheetController.getCellContent("B3"));

        typeInTextFieldEditor(b2, NUM_PROXY.toString());

        sheetController.selectCell("B3");
        sheetController.insertAndRet("=B2*2");

        Assert.assertEquals(NUM_PROXY.toString(),
                sheetController.getCellContent("B2"));
        Assert.assertEquals((NUM_PROXY * 2) + "",
                sheetController.getCellContent("B3"));
    }

    private void typeInTextFieldEditor(SheetCellElement cell, String text) {
        activateEditorInCell(cell);
        cell.findElement(By.xpath("./input")).clear();
        activateEditorInCell(cell);
        cell.findElement(By.xpath("./input")).sendKeys(text, Keys.RETURN);
    }

    private void activateEditorInCell(SheetCellElement cell) {
        cell.click();
        new Actions(getDriver()).moveToElement(cell).moveByOffset(7, 7)
                .doubleClick().build().perform();
    }

    @Test
    @Ignore("Fails with IE 9 and 10")
    public void testCheckBox() throws InterruptedException {
        loadServerFixture("CUSTOM_COMPONENTS");

        sheetController.selectCell("C3");
        sheetController.insertAndRet("=C2*2");
        sheetController.insertAndRet("=IF(C2,1,0)");

        sheetController.selectCell("A1");

        Assert.assertEquals("0", sheetController.getCellContent("C3"));
        Assert.assertEquals("0", sheetController.getCellContent("C4"));

        SheetCellElement c2 = $(SpreadsheetElement.class).first().getCellAt(
                "C2");
        c2.click();
        new Actions(getDriver())
                .moveToElement(c2.findElement(By.xpath(".//input"))).click()
                .build().perform();

        Assert.assertEquals("2", sheetController.getCellContent("C3"));
        Assert.assertEquals("1", sheetController.getCellContent("C4"));
    }

    @Test
    @Ignore("Fails with all IE and Phantom")
    public void testNativeSelect() {
        loadServerFixture("CUSTOM_COMPONENTS");

        sheetController.selectCell("I3");
        sheetController.insertAndRet("=I2*3");

        sheetController.selectCell("I2");
        Select select = new Select(driver.findElement(By.xpath(sheetController
                .cellToXPath("I2") + "//select")));
        select.getOptions().get(3).click();
        testBench(driver).waitForVaadin();

        Assert.assertEquals("90", sheetController.getCellContent("I3"));
    }

    @Test
    @Ignore("Fails with Phantom")
    public void testScrollingBug() throws InterruptedException {
        loadServerFixture("CUSTOM_COMPONENTS");

        SheetCellElement b2 = $(SpreadsheetElement.class).first().getCellAt(
                "B2");
        typeInTextFieldEditor(b2, TEXT_PROXY);
        sheetController.selectCell("B3");
        sheetController.selectCell("B2");

        Assert.assertEquals(TEXT_PROXY, b2.findElement(By.xpath("./input"))
                .getAttribute("value"));
        sheetController.selectCell("B5");
        sheetController.navigateToCell("B100");

        Sleeper.sleepTightInSeconds(1);
        sheetController.navigateToCell("B1");
        Sleeper.sleepTightInSeconds(3);

        b2 = $(SpreadsheetElement.class).first().getCellAt("B2");
        activateEditorInCell(b2);

        Assert.assertEquals(TEXT_PROXY, b2.findElement(By.xpath("./input"))
                .getAttribute("value"));
    }

    @Test
    @Ignore("Fails with IE 11")
    public void testButtonHandling() {
        loadServerFixture("CUSTOM_COMPONENTS");

        driver.findElement(By.id("b10-btn")).click();
        testBench(driver).waitForVaadin();
        Assert.assertEquals("42", sheetController.getCellContent("B11"));
        Assert.assertEquals("b12", driver.findElement(By.id("b12-label"))
                .getText());
    }

}
