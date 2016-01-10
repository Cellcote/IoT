/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.iot.parking;

/**
 *
 * @author rikschreurs
 */
public enum State {
    free ("free"), 
    reserved ("reserved"),
    occupied ("occupied");
    
    private final String stringRepresentation;
    State(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }
    
    public String toString() {
        return this.stringRepresentation;
    }
    
}
