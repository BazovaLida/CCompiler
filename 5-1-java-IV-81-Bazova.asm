.386
.model flat, stdcall
option casemap :none

include C:\masm32\include\kernel32.inc
include C:\masm32\include\user32.inc

includelib C:\masm32\lib\kernel32.lib
includelib C:\masm32\lib\user32.lib

main PROTO

.data
msg_title db "Result", 0
buffer db 128 dup(?)
format db "%d",0

.code
start:
	invoke main
	invoke wsprintf, addr buffer, addr format, eax
	invoke MessageBox, 0, addr buffer, addr msg_title, 0
	invoke ExitProcess, 0

main proc
	push ebp
	mov ebp, esp
	push 5
	push 10
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D0
	mov edx, 0ffffffffh
	jmp _D1
_D0:
	mov edx, 0
_D1:
	idiv ECX
	push EAX

	push 3
	push 2
	push 4
	pop ECX
	pop EAX
	mov EBX, EAX
	shr EBX, 31
	cmp EBX, 0
	je _D2
	mov edx, 0ffffffffh
	jmp _D3
_D2:
	mov edx, 0
_D3:
	idiv ECX
	push EAX

	pop EAX
	pop EBX
	add EAX, EBX
	push EAX
	pop ECX
	pop EAX
	cmp eax, 0   ; check if e1 is true
	jne _clause2   ; e1 is not 0, so we need to evaluate clause 2
	jmp _end
	_clause2:
		cmp ecx, 0 ; check if e2 is true
		mov eax, 0
		setne al

	_end:
		push eax

	push 7
	push 4
	pop EAX
	pop EBX
	add EAX, EBX
	push EAX
	pop ECX
	pop EAX
	cmp eax, 0   ; check if e1 is true
	jne _clause2   ; e1 is not 0, so we need to evaluate clause 2
	jmp _end
	_clause2:
		cmp ecx, 0 ; check if e2 is true
		mov eax, 0
		setne al

	_end:
		push eax

	pop [ebp-8];	i_var
	push 0
	pop [ebp-8];	i_var
_for_start1:
	mov ebx, [ebp-8]
	sub ebx, 1
	push 9
	pop eax	;expression
	cmp ebx, eax
	jle _for_end1	;if value less than expr

	push [ebp-8]     ;i_val
	push 1
	pop EAX
	pop EBX
	add EAX, EBX
	push EAX
	pop [ebp-8];	i_var
jmp _for_end1	;break
	push 10
	pop [ebp-12];	c_var
	jmp for_start1
_for_end1:
	push [ebp-8]     ;i_val
	pop eax ;here is the result
	mov esp, ebp	;restore ESP
	pop ebp	;restore old EBP
	ret 0

main ENDP
END start
