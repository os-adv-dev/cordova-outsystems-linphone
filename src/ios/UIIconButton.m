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

#import "UIIconButton.h"

#import "Utils.h"

@implementation UIIconButton

INIT_WITH_COMMON_CF {
	[super setImage:[self imageForState:UIControlStateNormal]
		   forState:(UIControlStateHighlighted | UIControlStateSelected)];
	[super setImage:[self imageForState:UIControlStateDisabled]
		   forState:(UIControlStateDisabled | UIControlStateSelected)];

	[self setBackgroundImage:[self backgroundImageForState:UIControlStateHighlighted]
					forState:(UIControlStateHighlighted | UIControlStateSelected)];
	[self setBackgroundImage:[self backgroundImageForState:UIControlStateDisabled]
					forState:(UIControlStateDisabled | UIControlStateSelected)];
	[self buttonFixStates:self];
	[self.titleLabel setAdjustsFontSizeToFitWidth:TRUE];

	return self;
}

- (void)buttonFixStates:(UIButton *)button {
    // Interface builder lack fixes
    [button setTitle:[button titleForState:UIControlStateSelected]
            forState:(UIControlStateHighlighted | UIControlStateSelected)];
    [button setTitleColor:[button titleColorForState:UIControlStateHighlighted]
                 forState:(UIControlStateHighlighted | UIControlStateSelected)];
    [button setTitle:[button titleForState:UIControlStateSelected]
            forState:(UIControlStateDisabled | UIControlStateSelected)];
    [button setTitleColor:[button titleColorForState:UIControlStateDisabled]
                 forState:(UIControlStateDisabled | UIControlStateSelected)];
}

- (void)setImage:(UIImage *)image forState:(UIControlState)state {
	[super setImage:image forState:state];
	[self commonInit];
}
@end
