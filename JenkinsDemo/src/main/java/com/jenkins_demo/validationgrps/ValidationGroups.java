package com.jenkins_demo.validationgrps;

import jakarta.validation.groups.Default;

public class ValidationGroups {
    // By extending Default, OnCreate now triggers both OnCreate AND all default un-grouped annotations
    public interface OnCreate extends Default {}
    public interface OnUpdate extends Default {}
}