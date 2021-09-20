/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-iphone
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#import "UIConfirmationDialog.h"
//#import "PhoneMainView.h"
#import "LinphoneManager.h"

@implementation UIConfirmationDialog
+ (UIConfirmationDialog *)initDialog:(NSString *)cancel
                           confirmMessage:(NSString *)confirm
                            onCancelClick:(UIConfirmationBlock)onCancel
                      onConfirmationClick:(UIConfirmationBlock)onConfirm
                             inController:(UIViewController *)controller {
    UIConfirmationDialog *dialog =
    [[UIConfirmationDialog alloc] initWithNibName:NSStringFromClass(self.class) bundle:NSBundle.mainBundle];
    
    //TODO
    //dialog.view.frame = PhoneMainView.instance.mainViewController.view.frame;
    [controller.view addSubview:dialog.view];
    [controller addChildViewController:dialog];
    
    dialog->onCancelCb = onCancel;
    dialog->onConfirmCb = onConfirm;
    
    if (cancel) {
        [dialog.cancelButton setTitle:cancel forState:UIControlStateNormal];
    }
    if (confirm) {
        [dialog.confirmationButton setTitle:confirm forState:UIControlStateNormal];
    }
    
    dialog.confirmationButton.layer.borderColor =
    [[UIColor colorWithPatternImage:[UIImage imageNamed:@"color_A.png"]] CGColor];
    dialog.cancelButton.layer.borderColor =
    [[UIColor colorWithPatternImage:[UIImage imageNamed:@"color_F.png"]] CGColor];
    return dialog;
}

+ (UIConfirmationDialog *)ShowWithMessage:(NSString *)message
							cancelMessage:(NSString *)cancel
						   confirmMessage:(NSString *)confirm
							onCancelClick:(UIConfirmationBlock)onCancel
					  onConfirmationClick:(UIConfirmationBlock)onConfirm
							 inController:(UIViewController *)controller {
	UIConfirmationDialog *dialog =
    [UIConfirmationDialog initDialog:cancel confirmMessage:confirm onCancelClick:onCancel onConfirmationClick:onConfirm inController:controller];
    [dialog.titleLabel setText:message];
	return dialog;
}

//TODO
/*
+ (UIConfirmationDialog *)ShowWithMessage:(NSString *)message
							cancelMessage:(NSString *)cancel
						   confirmMessage:(NSString *)confirm
							onCancelClick:(UIConfirmationBlock)onCancel
					  onConfirmationClick:(UIConfirmationBlock)onConfirm {
	return [self ShowWithMessage:message
				   cancelMessage:cancel
				  confirmMessage:confirm
				   onCancelClick:onCancel
			 onConfirmationClick:onConfirm
					inController:PhoneMainView.instance.mainViewController];
}*/
//TODO
/*
+ (UIConfirmationDialog *)ShowWithAttributedMessage:(NSMutableAttributedString *)attributedText
                            cancelMessage:(NSString *)cancel
                           confirmMessage:(NSString *)confirm
                            onCancelClick:(UIConfirmationBlock)onCancel
                      onConfirmationClick:(UIConfirmationBlock)onConfirm {
    UIConfirmationDialog *dialog =
    [UIConfirmationDialog initDialog:cancel confirmMessage:confirm onCancelClick:onCancel onConfirmationClick:onConfirm inController:PhoneMainView.instance.mainViewController];
    dialog.titleLabel.attributedText = attributedText;
    return dialog;
}*/

- (void)setSpecialColor {
	[_confirmationButton setBackgroundImage:[UIImage imageNamed:@"color_L.png"] forState:UIControlStateNormal];
	[_cancelButton setBackgroundImage:[UIImage imageNamed:@"color_I.png"] forState:UIControlStateNormal];
	[_cancelButton setTitleColor:[UIColor colorWithPatternImage:[UIImage imageNamed:@"color_H.png"]] forState:UIControlStateNormal];
	
	_confirmationButton.layer.borderColor =
	[[UIColor colorWithPatternImage:[UIImage imageNamed:@"color_L.png"]] CGColor];
	_cancelButton.layer.borderColor =
	[[UIColor colorWithPatternImage:[UIImage imageNamed:@"color_A.png"]] CGColor];
}

- (IBAction)onCancelClick:(id)sender {
	[self.view removeFromSuperview];
	[self removeFromParentViewController];
	if (onCancelCb) {
		onCancelCb();
	}
}

- (IBAction)onConfirmationClick:(id)sender {
	[self.view removeFromSuperview];
	[self removeFromParentViewController];
	if (onConfirmCb) {
		onConfirmCb();
	}
}

- (IBAction)onAuthClick:(id)sender {
    BOOL notAskAgain = true;
    UIImage *image = notAskAgain ? [UIImage imageNamed:@"checkbox_checked.png"] : [UIImage imageNamed:@"checkbox_unchecked.png"];
    [_authButton setImage:image forState:UIControlStateNormal];
}

- (void)dismiss {
	[self onCancelClick:nil];
}
@end
